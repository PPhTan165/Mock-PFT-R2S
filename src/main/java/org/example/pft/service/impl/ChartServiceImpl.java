package org.example.pft.service.impl;

import com.lowagie.text.Image;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.service.ChartService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ChartServiceImpl implements ChartService {
    private static final int CHART_WIDTH = 350;
    private static final int CHART_HEIGHT = 300;
    private static final int MONTHLY_CHART_WIDTH = 580;
    private static final int MONTHLY_CHART_HEIGHT = 300;

    @Override
    public Image createIncomeExpenseChart(SummaryData data) {
        if (data == null) {
            return null;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(toDouble(data.getIncome()), "Amount", "Income");
        dataset.addValue(toDouble(data.getExpense()), "Amount", "Expense");

        JFreeChart chart = ChartFactory.createBarChart(
                "Income & Expense - " + data.getMonth() + " " + data.getYear(),
                "Type",
                "Amount",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false
        );

        styleBarChart(chart);
        return convertToPdfImage(chart,480f,200f);
    }

    @Override
    public Image createTopExpensesChart(List<TopExpenses> topExpenses) {
        if (topExpenses == null || topExpenses.isEmpty()) {
            return null;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (TopExpenses topExpense : topExpenses) {
            dataset.addValue(toDouble(topExpense.getAmount()), "Amount", topExpense.getCategory());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top Expenses",
                "Category",
                "Amount",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                false,
                false
        );

        styleBarChart(chart);
        return convertToPdfImage(chart,480f,200f);
    }

    @Override
    public Image createMonthlyLineChart(List<ChartData> chartData, Integer year){
        if(chartData == null || chartData.isEmpty()){
            return null;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(ChartData item : chartData){
            dataset.addValue(
                    toDouble(item.getIncome()),
                    "Income",
                    item.getMonth()
            );

            dataset.addValue(
                    toDouble(item.getExpense()),
                    "Expenses",
                    item.getMonth()
            );

        }

        JFreeChart chart =
                ChartFactory.createLineChart(
                        "Monthly Income & Expense - " + year,
                        "Month",
                        "Amount",
                        dataset,
                        PlotOrientation.VERTICAL,
                        true,
                        true,
                        false
                );
        styleLineChart(chart);
        return convertLineToPdfImage(chart, 480f, 200f);
    }

    private void styleBarChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(0, 183, 255));
        renderer.setDrawBarOutline(false);
    }

    private void styleLineChart(JFreeChart chart){
        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(103, 102, 102, 190));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 10));
        domainAxis.setMaximumCategoryLabelWidthRatio(1.0f);
        domainAxis.setLowerMargin(0.02);
        domainAxis.setUpperMargin(0.02);
        domainAxis.setCategoryMargin(0.12);
    }

    private Image convertToPdfImage(JFreeChart chart, float width, float height) {
        BufferedImage bufferedImage = chart.createBufferedImage(CHART_WIDTH, CHART_HEIGHT);
        return convertToPdfImage(bufferedImage, width, height);
    }

    private Image convertLineToPdfImage(JFreeChart chart, float width, float height) {
        BufferedImage bufferedImage = chart.createBufferedImage(MONTHLY_CHART_WIDTH, MONTHLY_CHART_HEIGHT);
        return convertToPdfImage(bufferedImage, width, height);
    }

    private Image convertToPdfImage(BufferedImage bufferedImage, float width, float height) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", outputStream);
            Image image = Image.getInstance(outputStream.toByteArray());
            image.scaleToFit(width, height);
            image.setSpacingBefore(12f);
            image.setSpacingAfter(12f);
            return image;
        } catch (IOException ex) {
            throw new RuntimeException("Could not convert chart to PDF image", ex);
        }
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }
}
