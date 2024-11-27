import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class VkUserInfo {
    private static String TOKEN;
    private static final String API_VERSION = "5.131";

    static {
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            TOKEN = prop.getProperty("vk.api.token");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите имя: ");
        String firstName = scanner.nextLine();
        System.out.print("Введите фамилию: ");
        String lastName = scanner.nextLine();

        try {
            String userId = getUserId(firstName, lastName);
            if (userId != null) {
                JsonObject userInfo = getUserInfo(userId);
                if (userInfo != null) {
                    System.out.println("Возраст: " + userInfo.get("age").getAsString());
                    System.out.println("Город: " + userInfo.get("city").getAsString());
                } else {
                    System.out.println("Информация о пользователе недоступна");
                }
            } else {
                System.out.println("Пользователь не найден");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getUserId(String firstName, String lastName) throws IOException {
        String url = String.format("https://api.vk.com/method/users.search?q=%s+%s&access_token=%s&v=%s", firstName, lastName, TOKEN, API_VERSION);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
        if (jsonResponse.has("response") && jsonResponse.get("response").getAsJsonArray().size() > 0) {
            return jsonResponse.get("response").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
        }
        return null;
    }

    private static JsonObject getUserInfo(String userId) throws IOException {
        String url = String.format("https://api.vk.com/method/users.get?user_ids=%s&fields=bdate,city&access_token=%s&v=%s", userId, TOKEN, API_VERSION);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
        if (jsonResponse.has("response") && jsonResponse.get("response").getAsJsonArray().size() > 0) {
            JsonObject user = jsonResponse.get("response").getAsJsonArray().get(0).getAsJsonObject();
            JsonObject userInfo = new JsonObject();
            userInfo.addProperty("age", calculateAge(user.get("bdate").getAsString()));
            userInfo.addProperty("city", user.get("city").getAsJsonObject().get("title").getAsString());
            return userInfo;
        }
        return null;
    }

    private static String calculateAge(String bdate) {
        if (bdate != null && !bdate.isEmpty()) {
            String[] parts = bdate.split("\\.");
            if (parts.length >= 3) {
                try {
                    int year = Integer.parseInt(parts[2]);
                    LocalDate birthDate = LocalDate.of(year, Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                    int age = LocalDate.now().getYear() - birthDate.getYear();
                    if (LocalDate.now().getDayOfYear() < birthDate.getDayOfYear()) {
                        age--;
                    }
                    return String.valueOf(age);
                } catch (DateTimeParseException | NumberFormatException e) {
                    return "Возраст не определен";
                }
            }
        }
        return "Возраст не указан";
    }
}
