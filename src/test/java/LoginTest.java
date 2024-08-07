import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class LoginTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static int lastKnownTicketCount = 0;
    private static String lastTicketDescription = ""; // Переменная для хранения последнего описания заявки

    @BeforeClass
    public static void setUp() {
        System.setProperty("webdriver.chrome.driver", "C:/chromedriver/chromedriver.exe");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void testLogin() {
        if (driver == null) {
            System.out.println("Driver is null. Initializing...");
            setUp();
        }

        try {
            // Открываем страницу входа
            driver.get("https://helpdesk.ag-ife.com/site/login");

            // Вводим логин и пароль
            driver.findElement(By.id("LoginForm_username")).sendKeys("danil_ivanov");
            driver.findElement(By.id("LoginForm_password")).sendKeys("passwOrd1@3");

            // Нажимаем кнопку входа
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn-login"))).click();

            // Переходим на вкладку "Заявки"
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[.//span[contains(text(),'Заявки')]]"))).click();

            // Запоминаем начальное количество заявок
            lastKnownTicketCount = getTicketCount();
            lastTicketDescription = getFirstTicketDescription(); // Сохраняем начальное описание первой заявки

            // Запускаем функцию обновления страницы и проверки новых заявок
            refreshPageAndCheckNewTickets(60); // Обновляем страницу каждую минуту в течение 1 часа
        } catch (Exception e) {
            System.out.println("Ошибка при выполнении теста: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void refreshPageAndCheckNewTickets(int totalDurationMinutes) {
        long refreshInterval = 1 * 60 * 1000; // 1 минута в миллисекундах
        long totalDuration = totalDurationMinutes * 60 * 1000; // Общая продолжительность в миллисекундах
        long startTime = System.currentTimeMillis();
        long endTime = startTime + totalDuration;

        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(refreshInterval);
                driver.navigate().refresh();
                System.out.println("Страница обновлена: " + new java.util.Date());

                // Дождемся загрузки страницы после обновления
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("request-grid-full2")));

                // Проверяем наличие новых заявок
                int currentTicketCount = getTicketCount();
                String currentDescription = getFirstTicketDescription(); // Получаем текущее описание первой заявки

                // Проверяем, изменилось ли описание первой заявки
                if (!currentDescription.equals(lastTicketDescription)) {
                    System.out.println("Описание первой заявки изменилось!");
                    sendEmailNotification(currentTicketCount - lastKnownTicketCount); // Отправляем уведомление
                    lastTicketDescription = currentDescription; // Обновляем последнее описание
                }

                lastKnownTicketCount = currentTicketCount; // Обновляем количество заявок
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int getTicketCount() {
        try {
            // Ждем, пока таблица с заявками загрузится и станет видимой
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("request-grid-full2")));

            // Находим все строки таблицы
            List<WebElement> ticketRows = driver.findElements(By.cssSelector("#request-grid-full2 tbody tr"));

            // Выводим количество найденных строк для отладки
            System.out.println("Найдено заявок: " + ticketRows.size());

            // Возвращаем количество найденных строк
            return ticketRows.size();
        } catch (Exception e) {
            System.out.println("Ошибка при подсчете заявок: " + e.getMessage());
            return 0; // Возвращаем 0 в случае ошибки
        }
    }

    private String getFirstTicketDescription() {
        try {
            // Ждем, пока таблица с заявками загрузится и станет видимой
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("request-grid-full2")));

            // Находим первую строку таблицы
            WebElement firstRow = driver.findElement(By.cssSelector("#request-grid-full2 tbody tr:nth-child(1)"));
            // Получаем текст из второй ячейки (описание)
            return firstRow.findElement(By.cssSelector("td:nth-child(2)")).getText();
        } catch (Exception e) {
            System.out.println("Ошибка при получении описания первой заявки: " + e.getMessage());
            return ""; // Возвращаем пустую строку в случае ошибки
        }
    }

    private void sendEmailNotification(int newTicketCount) {
        // Настройки почтового сервера
        String host = "smtp.mail.ru"; // SMTP сервер
        final String user = "d.ivanov@kodoev.ru"; // Ваш email
        final String password = "e3KmunCP3Sf6actmN09e"; // Ваш пароль (проверьте, если требуется пароль приложения)

        if (user == null || user.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Email или пароль не установлены. Уведомление не отправлено.");
            return;
        }

        System.out.println("Attempting to send email notification...");
        System.out.println("SMTP Host: " + host);
        System.out.println("User: " + user);
        System.out.println("Password is set: " + (password != null && !password.isEmpty()));

        // Получаем свойства системы
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465"); // Порт для SSL
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.starttls.enable", "true"); // Включаем TLS

        // Получаем сессию
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        try {
            // Создаем объект сообщения
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user)); // Отправляем на тот же email
            message.setSubject("Новая заявка");
            message.setText("Helpdesk: " + newTicketCount + " новая заявка!");

            // Отправляем сообщение
            Transport.send(message);
            System.out.println("Уведомление отправлено на почту: " + newTicketCount + " новая заявка.");
        } catch (MessagingException e) {
            System.out.println("Ошибка при отправке уведомления: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
