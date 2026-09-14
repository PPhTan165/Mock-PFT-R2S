package org.example.pft.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessException;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.FileExportException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.PdfExportService;
import org.example.pft.service.ReportService;
import org.example.pft.service.pdf.CategoryReportData;
import org.example.pft.service.pdf.PdfExportContext;
import org.example.pft.service.pdf.PdfOptionalSectionRenderer;
import org.example.pft.service.pdf.PdfReportRenderer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {
    private final ReportService reportService;
    private final CurrentUserHelper currentUserHelper;
    private final PdfReportHelper pdfReportHelper;
    private final List<PdfReportRenderer> reportRenderers;
    private final List<PdfOptionalSectionRenderer> optionalSectionRenderers;

    // Generate a local PDF report from the validated request.
    @Override
    public ReportResponse<String> exportPDF(PdfExportRequest request) {
        validateRequest(request);

        User user = currentUserHelper.getCurrentUser();
        validateAuthenticatedUser(user);

        PdfExportContext context = buildPdfExportContext(request);
        Path filePath = createFilePath(request, user);

        writePdf(filePath, context);

        ReportResponse<String> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setMessage(buildResponseMessage(
                request,
                context.summaryData(),
                context.categoryReportData()
        ));
        response.setData(filePath.toString());

        return response;
    }

    private PdfReportRenderer getRenderer(ReportType reportType) {
        return reportRenderers.stream()
                .filter(renderer -> renderer.supportType() == reportType)
                .findFirst()
                .orElseThrow(() -> new BusinessValidationException("Unsupported report type "));
    }

    //  Map PdfExportRequest sang Record PdfExportContext
    private PdfExportContext buildPdfExportContext(PdfExportRequest request) {
        SummaryData summaryData = getSummaryData(request);
        List<ChartData> chartData = getMonthlyChartData(request);
        CategoryReportData categoryReportData = getCategoryReportData(request);

        return new PdfExportContext(
                request,
                summaryData,
                chartData,
                categoryReportData
        );
    }

    private void validateRequest(PdfExportRequest request) {
        if (request == null) {
            throw new BusinessValidationException("PDF export request is required");
        }

        if (request.getMonth() == null) {
            throw new BusinessValidationException("Month is required");
        }

        if (request.getMonth() < 1 || request.getMonth() > 12) {
            throw new BusinessValidationException("Month must be between 1 and 12");
        }

        if (request.getYear() == null) {
            throw new BusinessValidationException("Year is required");
        }

        if (request.getYear() < 1900 || request.getYear() > 9999) {
            throw new BusinessValidationException("Year must be between 1900 and 9999");
        }

        try {
            request.getReportType();
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Report type must be one of: SUMMARY, MONTHLY, CATEGORY");
        }
    }

    private void validateAuthenticatedUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessValidationException("Authenticated user is required to export PDF");
        }
    }

    private String buildResponseMessage(
            PdfExportRequest request,
            SummaryData data,
            CategoryReportData categoryReportData) {
        String monthYear = monthFullName(request.getMonth()) + " " + request.getYear();

        if (request.getReportType() == ReportType.CATEGORY && isEmptyCategoryReportData(categoryReportData)) {
            return "No category data found for " + monthYear;
        }

        if (request.getReportType() == ReportType.MONTHLY && isEmptySummary(data)) {
            return "No financial data found for " + monthYear;
        }

        String type = request.getReportType().toString().toLowerCase();
        return type + " report PDF generated successfully";
    }

    private Path createFilePath(PdfExportRequest request, User user) {
        try {
            // Ensure the local reports folder exists before writing the PDF file.
            Files.createDirectories(Path.of("reports"));

        } catch (IOException ex) {
            // Stop the export when the application cannot prepare the output folder.
            throw new RuntimeException("Could not create reports directory", ex);
        }

        String fileName = String.format(
                "%d_%s_%s_%d.pdf",
                user.getId(),
                request.getReportType().toString().toLowerCase(),
                monthShortName(request.getMonth()).toLowerCase(Locale.ENGLISH),
                request.getYear()
        );

        // Return the final local path where the PDF will be saved.
        return Path.of("reports", fileName);
    }

    private void writePdf(Path filePath, PdfExportContext context) {
        Path tempFile = null;

        try {
            validateTargetFile(filePath);
            tempFile = Files.createTempFile(filePath.getParent(), filePath.getFileName().toString(), ".tmp");
            writePdfContent(tempFile, context);
            replacePdfFile(tempFile, filePath);
            tempFile = null;

        } catch (BusinessException ex) {
            throw ex;
        } catch (DocumentException ex) {
            throw new RuntimeException("Could not generate PDF report", ex);
        } catch (IOException ex) {
            throw mapFileWriteException(ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void writePdfContent(
            Path filePath,
            PdfExportContext context)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);

        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            PdfReportRenderer renderer = getRenderer(context.request().getReportType());

            writeHeader(document, context, renderer);
            renderer.render(document, context);
            writeOptionalSections(document, context);

            document.close();
        } finally {
            // Close the document before closing the stream so OpenPDF can finish writing the file.
            if (document.isOpen()) {
                document.close();
            }

        }
    }

    //  HEADER TABLE ====================================
    private void writeHeader(Document document, PdfExportContext context, PdfReportRenderer renderer) throws DocumentException {
        PdfExportRequest request = context.request();

        pdfReportHelper.addTitle(
                document,
                renderer.title()
                        + " - "
                        + monthFullName(request.getMonth())
                        + " "
                        + request.getYear()
        );
    }

    //  OPTIONAL SECTION RENDERER =========================
    private void writeOptionalSections(Document document, PdfExportContext context) throws DocumentException {
        for (PdfOptionalSectionRenderer sectionRenderer : optionalSectionRenderers) {
            if (sectionRenderer.supports(context)) {
                sectionRenderer.render(document, context);
            }
        }
    }

    private RuntimeException mapFileWriteException(IOException ex) {
        if (ex instanceof AccessDeniedException || ex instanceof FileSystemException) {
            return new FileExportException("Report PDF file cannot be written. Close it if it is open and try again.");
        }

        return new RuntimeException("Could not generate PDF report", ex);
    }

    //    GET DATA TABLE ==========================================
    private SummaryData getSummaryData(PdfExportRequest request) {
        ReportResponse<SummaryData> response = reportService.showSummary(request.getMonth(), request.getYear());
        return response == null ? null : response.getData();
    }

    private List<ChartData> getMonthlyChartData(PdfExportRequest request) {
        ReportResponse<MonthlyData> response = reportService.showMonthly(request.getMonth(), request.getYear());
        MonthlyData data = response == null ? null : response.getData();
        return data == null || data.getChart() == null ? List.of() : data.getChart();
    }

    private List<ReportCategory> getCategoryPieChartData(PdfExportRequest request, CategoryType type) {
        ReportResponse<ReportCategoryData> response = reportService.showReportCategory(request.getMonth(), request.getYear(), type);
        ReportCategoryData data = response == null ? null : response.getData();
        return data == null || data.getCategories() == null ? List.of() : data.getCategories();
    }

    private CategoryReportData getCategoryReportData(PdfExportRequest request) {
        if (request.getReportType() != ReportType.CATEGORY) {
            return new CategoryReportData(List.of(), List.of());
        }

        return new CategoryReportData(
                getCategoryPieChartData(request, CategoryType.EXPENSE),
                getCategoryPieChartData(request, CategoryType.INCOME)
        );
    }

    //    VALIDATION ===========================================
    private boolean isEmptyCategoryReportData(CategoryReportData data) {
        return data == null
                || (data.expenseCategories() == null || data.expenseCategories().isEmpty())
                && (data.incomeCategories() == null || data.incomeCategories().isEmpty());
    }

    private void validateTargetFile(Path filePath) {
        if (Files.exists(filePath) && !Files.isRegularFile(filePath)) {
            throw new FileExportException("Report PDF path is not a writable file.");
        }
    }

    private void replacePdfFile(Path tempFile, Path filePath) throws IOException {
        try {
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw mapFileWriteException(ex);
        }
    }

    private boolean isEmptySummary(SummaryData data) {
        return data == null
                || amount(data.getIncome()).compareTo(BigDecimal.ZERO) == 0
                && amount(data.getExpense()).compareTo(BigDecimal.ZERO) == 0
                && (data.getTopExpenses() == null || data.getTopExpenses().isEmpty());
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String monthShortName(Integer month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private String monthFullName(Integer month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }


}
