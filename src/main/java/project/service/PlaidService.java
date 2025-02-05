package project.service;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
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
import com.plaid.client.model.Products;
import static com.plaid.client.model.CountryCode.US;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaidService {
    private final PlaidApi plaidApi;
    private final PlaidItemRepository plaidItemRepository;
    private final UserRepository userRepository;


    private String getErrorMessage(Response<?> response) {
        if (response.errorBody() == null) {
            return "Unknown error";
        }

        try (ResponseBody errorBody = response.errorBody()) {
            return errorBody.string();
        } catch (IOException e) {
            log.error("Error reading error body", e);
            return "Error reading error response";
        }
    }

    public LinkTokenCreateResponse createLinkToken(String userEmail) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String clientUserId = "user-" + user.getId();


        LinkTokenCreateRequestUser requestUser = new LinkTokenCreateRequestUser()
                .clientUserId(clientUserId);

        List<Products> products = Arrays.asList(
                Products.TRANSACTIONS,
                Products.AUTH
        );

        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(requestUser)
                .clientName("Finance App")
                .products(products)
                .countryCodes(Arrays.asList(CountryCode.US))
                .language("en");

        Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();

        if (!response.isSuccessful() || response.body() == null) {
            String errorMessage = getErrorMessage(response);
            log.error("Error creating link token: {}", errorMessage);
            throw new RuntimeException("Error creating link token: " + errorMessage);
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

        if (!response.isSuccessful() || response.body() == null) {
            String errorMessage = getErrorMessage(response);
            log.error("Error exchanging public token: {}", errorMessage);
            throw new RuntimeException("Error exchanging public token: " + errorMessage);
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

            if (!response.isSuccessful() || response.body() == null) {
                String errorMessage = getErrorMessage(response);
                log.error("Error getting accounts for item {}: {}", item.getItemId(), errorMessage);
                continue;
            }

            allAccounts.addAll(response.body().getAccounts());
        }

        return allAccounts;
    }

    public List<PlaidItem> getUserPlaidItems(String userEmail) {
        return plaidItemRepository.findByUserEmail(userEmail);
    }
    public String createSandboxPublicToken() throws IOException {
        SandboxPublicTokenCreateRequest request = new SandboxPublicTokenCreateRequest()
                .institutionId("ins_109508")  // Chase Bank sandbox institution ID
                .initialProducts(Arrays.asList(Products.TRANSACTIONS, Products.AUTH));

        Response<SandboxPublicTokenCreateResponse> response =
                plaidApi.sandboxPublicTokenCreate(request).execute();

        if (!response.isSuccessful() || response.body() == null) {
            String errorMessage = getErrorMessage(response);
            log.error("Error creating sandbox public token: {}", errorMessage);
            throw new RuntimeException("Error creating sandbox public token: " + errorMessage);
        }

        return response.body().getPublicToken();
    }
}