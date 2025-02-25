package com.myProject.spamDetector;

import jakarta.mail.*;
import jakarta.mail.Flags.Flag;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;

import java.io.IOException;
import java.util.Properties;

public class GmailServiceWrapper {
    private static String email;
    private static String appPassword;

    public static void setCredentials(String userEmail, String userPassword) {
        email = userEmail;
        appPassword = userPassword;
    }

    public static String readMessagesFromGmail() throws Exception {
        if (email == null || appPassword == null) {
            throw new IllegalStateException("Email credentials not set, please enter your Gmail address and app password first!");
        }

        Store store = null;
        Folder folder = null;
        String output = "";

        try {
            store = getImapStore();
            folder = getFolderFromStore(store, "INBOX");

            Message[] messages = folder.search(getUnreadSearchTerm());

            if (messages.length == 0) {
                return "No unread emails";
            } else {
                folder.fetch(messages, getFetchProfile());

                // Fetch only the first unread email
                Message message = messages[0];
                output = printMessage(message);

                // Mark the message as read
                message.setFlag(Flag.SEEN, true);
            }
        } catch (MessagingException e) {
            throw new Exception("Mail read failed: " + e.getMessage());
        } finally {
            closeFolder(folder);
            closeStore(store);
        }

        return output;
    }

    private static SearchTerm getUnreadSearchTerm() {
        return new FlagTerm(new Flags(Flag.SEEN), false);
    }

    private static Store getImapStore() throws MessagingException {
        Session session = Session.getInstance(getImapProperties());
        Store store = session.getStore("imaps");
        store.connect("imap.gmail.com", email, appPassword);
        return store;
    }

    private static Properties getImapProperties() {
        Properties props = new Properties();
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.ssl.trust", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.starttls.enable", "true");
        return props;
    }

    private static Folder getFolderFromStore(Store store, String folderName) throws MessagingException {
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_WRITE);
        return folder;
    }

    private static FetchProfile getFetchProfile() {
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
        return fetchProfile;
    }

    private static String printMessage(Message message) throws MessagingException, IOException {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("SUBJECT: ").append(message.getSubject()).append("\n");

        StringBuilder textCollector = new StringBuilder();
        collectTextFromMessage(textCollector, message);
        messageBuilder.append("TEXT: ").append(textCollector.toString()).append("\n");

        return messageBuilder.toString();
    }

    private static void collectTextFromMessage(StringBuilder textCollector, Part part)
            throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            textCollector.append((String) part.getContent());
        } else if (part.isMimeType("multipart/*")) {
            Multipart multiPart = (Multipart) part.getContent();
            for (int i = 0; i < multiPart.getCount(); i++) {
                collectTextFromMessage(textCollector, multiPart.getBodyPart(i));
            }
        }
    }

    private static void closeFolder(Folder folder) {
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(true);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    private static void closeStore(Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
