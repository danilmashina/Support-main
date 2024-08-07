import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait; 

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class LoginTest {

    private static WebDriver driver;
    private static Wait<WebDriver> wait;
    private static int lastKnownTicketCount = 0;
    private static String lastTicketDescription = "";

    @BeforeClass
    public static void setUp() {
        System.setProperty("webdriver.chrome.driver", "C:/chromedriver/chromedriver.exe");
        driver = new ChromeDriver();
        wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(NoSuchElementException.class);
    }

    @Test
    public void testLogin() {
        try {
            driver.get("https://helpdesk.ag-ife.com/site/login");
            driver.findElement(By.id("LoginForm_username")).sendKeys("danil_ivanov");
            driver.findElement(By.id("LoginForm_password")).sendKeys("passwOrd1@3");
            wait.until(driver -> driver.findElement(By.cssSelector("button.btn-login"))).click();
            wait.until(driver -> driver.findElement(By.xpath("//a[.//span[contains(text(),'Заявки')]]"))).click();
            
            lastKnownTicketCount = getTicketCount();
            lastTicketDescription = getFirstTicketDescription();
            
            refreshPageAndCheckNewTickets(60);
        } catch (Exception e) {
            System.out.println("Ошибка при выполнении теста: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private int getTicketCount() {
        try {
            wait.until(driver -> driver.findElement(By.id("request-grid-full2")));
            List<WebElement> ticketRows = driver.findElements(By.cssSelector("#request-grid-full2 tbody tr"));
            System.out.println("Найдено заявок: " + ticketRows.size());
            return ticketRows.size();
        } catch (Exception e) {
            System.out.println("Ошибка при подсчете заявок: " + e.getMessage());
            return 0;
        }
    }

    private String getFirstTicketDescription() {
        try {
            wait.until(driver -> driver.findElement(By.id("request-grid-full2")));
            WebElement firstRow = driver.findElement(By.cssSelector("#request-grid-full2 tbody tr:nth-child(1)"));
            return firstRow.findElement(By.cssSelector("td:nth-child(2)")).getText();
        } catch (Exception e) {
            System.out.println("Ошибка при получении описания первой заявки: " + e.getMessage());
            return "";
        }
    }

    private void refreshPageAndCheckNewTickets(int totalDurationMinutes) {
        long refreshInterval = 1 * 60 * 1000;
        long totalDuration = totalDurationMinutes * 60 * 1000;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + totalDuration;

        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(refreshInterval);
                driver.navigate().refresh();
                System.out.println("Страница обновлена: " + new java.util.Date());

                wait.until(driver -> driver.findElement(By.id("request-grid-full2")));

                int currentTicketCount = getTicketCount();
                String currentDescription = getFirstTicketDescription();

                if (!currentDescription.equals(lastTicketDescription)) {
                    System.out.println("Описание первой заявки изменилось!");
                    sendEmailNotification(currentTicketCount - lastKnownTicketCount);
                    lastTicketDescription = currentDescription;
                }

                lastKnownTicketCount = currentTicketCount;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEmailNotification(int newTicketCount) {
        String host = "smtp.mail.ru";
        final String user = System.getenv("d.ivanov@kodoev.ru");
        final String password = System.getenv("e3KmunCP3Sf6actmN09e");

        if (user == null || user.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Email или пароль не установлены. Уведомление не отправлено.");
            return;
        }

        System.out.println("Email user: " + user);
        System.out.println("Email password: " + (password != null ? "set" : "not set"));

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user));
            message.setSubject("Новая заявка");
            message.setText("Helpdesk: " + newTicketCount + " новая заявка!");

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
        }
    }
}
