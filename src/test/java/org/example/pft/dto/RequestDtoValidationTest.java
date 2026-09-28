package org.example.pft.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.pft.dto.auth.RegisterRequest;
import org.example.pft.dto.email.EmailExportRequest;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.transaction.HistoryRequest;
import org.example.pft.dto.transaction.TransactionRequest;
import org.example.pft.enums.CategoryType;
import org.example.pft.enums.ReportType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void registerRequest_withPasswordShorterThanMinimum_shouldViolateSize() {
        RegisterRequest request = validRegisterRequest();
        request.setPassword("abc1234");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertViolation(violations, "password", Size.class);
    }

    @Test
    void registerRequest_withPasswordWithoutDigit_shouldViolatePattern() {
        RegisterRequest request = validRegisterRequest();
        request.setPassword("abcdefgh");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertViolation(
                violations,
                "password",
                Pattern.class,
                "Password must be at least 8 characters and contain both letters and numbers"
        );
    }

    @Test
    void registerRequest_withPasswordAtMinimumAndRequiredComposition_shouldBeValid() {
        RegisterRequest request = validRegisterRequest();
        request.setPassword("abcd1234");

        assertValid(request);
    }

    @Test
    void transactionRequest_withAmountBelowMinimumAndNoteTooLong_shouldReportFieldViolations() {
        TransactionRequest request = validTransactionRequest();
        request.setAmount(new BigDecimal("0.00"));
        request.setNote("x".repeat(256));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertViolation(violations, "amount", DecimalMin.class, "Amount must be greater than 0");
        assertViolation(violations, "note", Size.class, "Note must not exceed 255 characters");
    }

    @Test
    void transactionRequest_withMinimumAmountAndMaxLengthNote_shouldBeValid() {
        TransactionRequest request = validTransactionRequest();
        request.setAmount(new BigDecimal("0.01"));
        request.setNote("x".repeat(255));

        assertValid(request);
    }

    @Test
    void historyRequest_withInvalidPaginationAndDateRange_shouldReportBoundaryViolations() {
        HistoryRequest request = validHistoryRequest();
        request.setStartDate(LocalDate.of(2026, 9, 30));
        request.setEndDate(LocalDate.of(2026, 9, 1));
        request.setPage(0);
        request.setSize(HistoryRequest.MAX_SIZE + 1);

        Set<ConstraintViolation<HistoryRequest>> violations = validator.validate(request);

        assertViolation(violations, "page", Min.class, "Page must be greater than or equal to 1");
        assertViolation(violations, "size", Max.class, "Size must be less than or equal to 20");
        assertViolation(violations, "validDateRange", AssertTrue.class, "Start date must be before or equal to end date");
    }

    @Test
    void historyRequest_withBoundaryPaginationAndEqualDates_shouldBeValid() {
        HistoryRequest request = validHistoryRequest();
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 9, 1));
        request.setPage(1);
        request.setSize(HistoryRequest.MAX_SIZE);

        assertValid(request);
    }

    @Test
    void emailExportRequest_withValuesBelowMinimum_shouldReportMinViolations() {
        EmailExportRequest request = validEmailExportRequest();
        request.setMonth(0);
        request.setYear(1899);

        Set<ConstraintViolation<EmailExportRequest>> violations = validator.validate(request);

        assertViolation(violations, "month", Min.class, "Month must be between 1 and 12");
        assertViolation(violations, "year", Min.class, "Year must be between 1900 and 9999");
    }

    @Test
    void emailExportRequest_withValuesAboveMaximumAndInvalidEmail_shouldReportViolations() {
        EmailExportRequest request = validEmailExportRequest();
        request.setMonth(13);
        request.setYear(10000);
        request.setEmail("not-an-email");

        Set<ConstraintViolation<EmailExportRequest>> violations = validator.validate(request);

        assertViolation(violations, "month", Max.class, "Month must be between 1 and 12");
        assertViolation(violations, "year", Max.class, "Year must be between 1900 and 9999");
        assertViolation(violations, "email", Email.class, "Email format is invalid");
    }

    @Test
    void emailExportRequest_withUpperBoundaryValues_shouldBeValid() {
        EmailExportRequest request = validEmailExportRequest();
        request.setMonth(12);
        request.setYear(9999);

        assertValid(request);
    }

    @Test
    void pdfExportRequest_withInvalidReportType_shouldViolatePattern() {
        PdfExportRequest request = validPdfExportRequest();
        request.setReportType("weekly");

        Set<ConstraintViolation<PdfExportRequest>> violations = validator.validate(request);

        assertViolation(
                violations,
                "reportType",
                Pattern.class,
                "Report type must be one of: SUMMARY, MONTHLY, CATEGORY"
        );
    }

    @Test
    void pdfExportRequest_withBlankReportType_shouldDefaultToSummaryAndBeValid() {
        PdfExportRequest request = validPdfExportRequest();
        request.setReportType("   ");

        assertValid(request);
        assertEquals(ReportType.SUMMARY, request.getReportType());
    }

    @Test
    void pdfExportRequest_withCaseInsensitiveReportType_shouldBeValid() {
        PdfExportRequest request = validPdfExportRequest();
        request.setReportType("monthly");

        assertValid(request);
        assertEquals(ReportType.MONTHLY, request.getReportType());
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("abcd1234");
        request.setFullName("Test User");
        return request;
    }

    private TransactionRequest validTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setNote("Lunch");
        request.setCategoryId(20L);
        request.setDate(LocalDate.of(2026, 9, 7));
        return request;
    }

    private HistoryRequest validHistoryRequest() {
        HistoryRequest request = new HistoryRequest();
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 9, 30));
        request.setType(CategoryType.EXPENSE);
        request.setPage(1);
        request.setSize(10);
        return request;
    }

    private EmailExportRequest validEmailExportRequest() {
        EmailExportRequest request = new EmailExportRequest();
        request.setMonth(9);
        request.setYear(2026);
        request.setEmail("user@example.com");
        return request;
    }

    private PdfExportRequest validPdfExportRequest() {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(9);
        request.setYear(2026);
        request.setReportType(ReportType.SUMMARY);
        return request;
    }

    private <T> void assertValid(T request) {
        assertTrue(validator.validate(request).isEmpty());
    }

    private <T> void assertViolation(
            Set<ConstraintViolation<T>> violations,
            String property,
            Class<? extends Annotation> annotationType
    ) {
        assertTrue(
                hasViolation(violations, property, annotationType),
                () -> "Expected " + annotationType.getSimpleName() + " violation for " + property
        );
    }

    private <T> void assertViolation(
            Set<ConstraintViolation<T>> violations,
            String property,
            Class<? extends Annotation> annotationType,
            String message
    ) {
        assertTrue(
                violations.stream().anyMatch(violation ->
                        property.equals(violation.getPropertyPath().toString())
                                && annotationType.equals(violation.getConstraintDescriptor().getAnnotation().annotationType())
                                && message.equals(violation.getMessage())),
                () -> "Expected " + annotationType.getSimpleName() + " violation for " + property
                        + " with message " + message
        );
    }

    private <T> boolean hasViolation(
            Set<ConstraintViolation<T>> violations,
            String property,
            Class<? extends Annotation> annotationType
    ) {
        return violations.stream().anyMatch(violation ->
                property.equals(violation.getPropertyPath().toString())
                        && annotationType.equals(violation.getConstraintDescriptor().getAnnotation().annotationType()));
    }
}
