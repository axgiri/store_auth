package com.github.oldlabauth.dto.request;

import com.github.oldlabauth.dto.MessageChannelEnum;
import com.github.oldlabauth.entity.OtpType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OtpValidationRequest(
    
    MessageChannelEnum channel,

    @NotNull(message = "contact cannot be null")
    String contact,

    OtpType otpType,

    @NotNull(message = "otp cannot be null")
    @Min(value = 1000, message = "OTP must be 4 digits")
    @Max(value = 9999, message = "OTP must be 4 digits")
    Integer otp
) {
    public static OtpValidationRequest forEmailActivation(String email, int otp) {
        return new OtpValidationRequest(
            MessageChannelEnum.EMAIL, 
            email, 
            OtpType.ACTIVATE_ACCOUNT, 
            otp
        );
    }

    public static OtpValidationRequest forEmailPasswordReset(String email, int otp) {
        return new OtpValidationRequest(
            MessageChannelEnum.EMAIL, 
            email, 
            OtpType.RESET_PASSWORD, 
            otp
        );
    }

    public static OtpValidationRequest forEmailLogin(String email, int otp) {
        return new OtpValidationRequest(
            MessageChannelEnum.EMAIL, 
            email, 
            OtpType.LOGIN, 
            otp
        );
    }
}
