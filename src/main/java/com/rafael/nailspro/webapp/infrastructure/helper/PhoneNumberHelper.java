package com.rafael.nailspro.webapp.infrastructure.helper;

public class PhoneNumberHelper {

    public static String formatPhoneNumber(String phoneNumber) {
        String cleanNumber = phoneNumber.replaceAll("\\D", "");
        return cleanNumber.startsWith("55")
                ? cleanNumber
                : "55" + cleanNumber;
    }
}
