package com.github.storeauth.dto.request;

import com.github.storeauth.dto.MessageChannelEnum;
import com.github.storeauth.entity.OtpType;

public record OtpRequest(
    MessageChannelEnum channel,
    String contact,
    OtpType otpType
) {
    public static OtpRequest email(String email, OtpType otpType) {
        return new OtpRequest(MessageChannelEnum.EMAIL, email, otpType);
    }

    public static OtpRequest sms(String phone, OtpType otpType) {
        return new OtpRequest(MessageChannelEnum.SMS, phone, otpType);
    }
}
