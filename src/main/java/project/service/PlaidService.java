package project.service;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.model.PlaidItem;
import project.model.User;
import project.repository.PlaidItemRepository;
import project.repository.UserRepository;
import retrofit2.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaidService {
    private final PlaidApi plaidApi;
    private final PlaidItemRepository plaidItemRepository;
    private final UserRepository userRepository;

    public LinkTokenCreateResponse createLinkToken(String userEmail) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create user object
        LinkTokenCreateRequestUser requestUser = new LinkTokenCreateRequestUser()
                .clientUserId(userEmail);

        // Configure the products you want to use
        List<Products> products = Arrays.asList(
                Products.TRANSACTIONS,
                Products.AUTH
        );

        // Create config
        LinkTokenCreateRequest.ConfigurationObject config = new LinkTokenCreateRequest.ConfigurationObject();

        // Create the link_token request
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(requestUser)
                .clientName("Your App Name")
                .products(products)
                .countryCodes(Arrays.asList("US"))
                .language("en")
                .webhook("https://yourapp.com/webhook")
                .redirectUri("https://yourapp.com/oauth-redirect")
                .configuration(config);

        Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();

        if (!response.isSuccessful()) {
            PlaidError error = parseError(response);
            throw new RuntimeException("Error creating link token: " + error.getErrorMessage());
        }

        return response.body();
    }

    public String exchangePublicToken(String publicToken, String userEmail,
                                      String institutionId, String institutionName) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
                .publicToken(publicToken);

        Response<ItemPublicTokenExchangeResponse> response =
                plaidApi.itemPublicTokenExchange(request).execute();

        if (!response.isSuccessful()) {
            PlaidError error = parseError(response);
            throw new RuntimeException("Error exchanging public token: " + error.getErrorMessage());
        }

        String accessToken = response.body().getAccessToken();
        String itemId = response.body().getItemId();

        // Save Plaid item
        PlaidItem plaidItem = new PlaidItem();
        plaidItem.setUser(user);
        plaidItem.setItemId(itemId);
        plaidItem.setAccessToken(accessToken);
        plaidItem.setInstitutionId(institutionId);
        plaidItem.setInstitutionName(institutionName);

        plaidItemRepository.save(plaidItem);

        return accessToken;
    }

    public List<AccountBase> getAccounts(String userEmail) throws IOException {
        List<PlaidItem> plaidItems = plaidItemRepository.findByUserEmail(userEmail);
        List<AccountBase> allAccounts = new ArrayList<>();

        for (PlaidItem item : plaidItems) {
            AccountsGetRequest request = new AccountsGetRequest()
                    .accessToken(item.getAccessToken());

            Response<AccountsGetResponse> response = plaidApi.accountsGet(request).execute();

            if (!response.isSuccessful()) {
                PlaidError error = parseError(response);
                log.error("Error getting accounts for item {}: {}",
                        item.getItemId(), error.getErrorMessage());
                continue;
            }

            allAccounts.addAll(response.body().getAccounts());
        }

        return allAccounts;
    }

    public List<PlaidItem> getUserPlaidItems(String userEmail) {
        return plaidItemRepository.findByUserEmail(userEmail);
    }

    private PlaidError parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return PlaidError.fromJson(response.errorBody().string());
            }
        } catch (IOException e) {
            log.error("Error parsing Plaid error response", e);
        }
        return new PlaidError().errorMessage("Unknown error occurred");
    }
}