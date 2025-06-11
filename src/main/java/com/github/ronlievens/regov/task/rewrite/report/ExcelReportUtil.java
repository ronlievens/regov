package com.github.ronlievens.regov.task.rewrite.report;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.nio.file.Path;

import static com.github.ronlievens.regov.util.PathUtils.createDirectory;
import static com.github.ronlievens.regov.util.PathUtils.getFileFromClasspath;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExcelReportUtil {

    private static final String EXCEL_TEMPLATE = "templates/report.xlsx";

    public static void createExcelReport(final String ticketUrl, @NonNull final Path output, @NonNull final ReportModel reportModel) {
        try {
            val fileIS = getFileFromClasspath(EXCEL_TEMPLATE);
            val workbook = new XSSFWorkbook(fileIS);
            val sheet = workbook.getSheetAt(0);

            val whiteFont = workbook.createFont();
            whiteFont.setColor(IndexedColors.WHITE.index);

            val blackFont = workbook.createFont();
            blackFont.setColor(IndexedColors.BLACK.index);

            val bgGreen = workbook.createCellStyle();
            bgGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bgGreen.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            bgGreen.setFont(whiteFont);

            val bgBlue = workbook.createCellStyle();
            bgBlue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bgBlue.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            bgBlue.setFont(whiteFont);

            val bgYellow = workbook.createCellStyle();
            bgYellow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bgYellow.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            bgYellow.setFont(blackFont);

            val bgOrange = workbook.createCellStyle();
            bgOrange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bgOrange.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            bgOrange.setFont(whiteFont);

            val bgRed = workbook.createCellStyle();
            bgRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bgRed.setFillForegroundColor(IndexedColors.RED.getIndex());
            bgRed.setFont(whiteFont);

            val bgFalse = workbook.createCellStyle();
            bgFalse.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bgFalse.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            bgFalse.setFont(blackFont);

            // C2 ticket
            var row = sheet.getRow(0);
            var cell = row.createCell(2);
            cell.setCellValue(reportModel.getTicket());
            var link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
            if (isNotBlank(ticketUrl)) {
                link.setAddress(ticketUrl.formatted(reportModel.getTicket()));
                cell.setHyperlink(link);
            } else {
                cell.setCellValue(reportModel.getTicket());
            }

            var rowCounter = 4;
            for (val repository : reportModel.getRepositories()) {
                row = sheet.createRow(rowCounter);

                // Repository name
                var cellIndex = 0;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getName());

                // Project name
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getProjectName());

                // Change - commit
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getChange().isCommit());
                if (repository.getChange().isCommit()) {
                    cell.setCellStyle(bgGreen);
                } else {
                    cell.setCellStyle(bgRed);
                }

                // Change - merge
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getChange().isMerged());
                if (repository.getChange().isMerged()) {
                    cell.setCellStyle(bgGreen);
                } else if (repository.getChange().isCommit()) {
                    cell.setCellStyle(bgOrange);
                } else {
                    cell.setCellStyle(bgRed);
                }

                // Change - pull request
                cellIndex++;
                cell = row.createCell(cellIndex);
                if (repository.getChange().isCommit()) {
                    cell.setCellValue("%s [%s]".formatted(repository.getChange().getPullRequestStatus(), repository.getChange().getPullRequestId()));
                    link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(repository.getChange().getPullRequestViewUrl());
                    cell.setHyperlink(link);
                    if (repository.getChange().isMerged()) {
                        cell.setCellStyle(bgGreen);
                    } else if (repository.getChange().isCommit()) {
                        cell.setCellStyle(bgOrange);
                    } else {
                        cell.setCellStyle(bgRed);
                    }
                }

                // Change - build
                cellIndex++;
                cell = row.createCell(cellIndex);
                if (repository.getChange().hasBuild()) {
                    cell.setCellValue("%s [%s]".formatted(repository.getChange().getBuildStatus(), repository.getChange().getBuildId()));
                    link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(repository.getChange().getBuildViewUrl());
                    cell.setHyperlink(link);
                    if (repository.getChange().isBuildSuccess()) {
                        cell.setCellStyle(bgGreen);
                    } else if (repository.getChange().isBuildInProgress()) {
                        cell.setCellStyle(bgBlue);
                    } else if (repository.getChange().isBuildFailed()) {
                        cell.setCellStyle(bgRed);
                    } else {
                        cell.setCellStyle(bgOrange);
                    }
                }


                // Acceptance - commit
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getAcceptance().isCommit());
                if (repository.getAcceptance().isCommit()) {
                    cell.setCellStyle(bgGreen);
                } else {
                    cell.setCellStyle(bgRed);
                }

                // Acceptance - merge
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getAcceptance().isMerged());
                if (repository.getAcceptance().isMerged()) {
                    cell.setCellStyle(bgGreen);
                } else if (repository.getAcceptance().isCommit()) {
                    cell.setCellStyle(bgOrange);
                } else {
                    cell.setCellStyle(bgRed);
                }

                // Acceptance - pull request
                cellIndex++;
                cell = row.createCell(cellIndex);
                if (repository.getAcceptance().isCommit()) {
                    cell.setCellValue("%s [%s]".formatted(repository.getAcceptance().getPullRequestStatus(), repository.getAcceptance().getPullRequestId()));
                    link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(repository.getAcceptance().getPullRequestViewUrl());
                    cell.setHyperlink(link);
                    if (repository.getAcceptance().isMerged()) {
                        cell.setCellStyle(bgGreen);
                    } else if (repository.getAcceptance().isCommit()) {
                        cell.setCellStyle(bgOrange);
                    } else {
                        cell.setCellStyle(bgRed);
                    }
                }

                // Acceptance - original container
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getAcceptance().getContainerOriginalVersion());

                // Acceptance - new container
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getAcceptance().getContainerNewVersion());

                // Production- commit
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getProduction().isCommit());
                if (repository.getProduction().isCommit()) {
                    cell.setCellStyle(bgGreen);
                } else {
                    cell.setCellStyle(bgRed);
                }

                // Production - merge
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getProduction().isMerged());
                if (repository.getProduction().isMerged()) {
                    cell.setCellStyle(bgGreen);
                } else if (repository.getProduction().isCommit()) {
                    cell.setCellStyle(bgOrange);
                } else {
                    cell.setCellStyle(bgRed);
                }

                // Production - pull request
                cellIndex++;
                cell = row.createCell(cellIndex);
                if (repository.getProduction().isCommit()) {
                    cell.setCellValue("%s [%s]".formatted(repository.getProduction().getPullRequestStatus(), repository.getProduction().getPullRequestId()));
                    link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(repository.getProduction().getPullRequestViewUrl());
                    cell.setHyperlink(link);
                    if (repository.getProduction().isMerged()) {
                        cell.setCellStyle(bgGreen);
                    } else if (repository.getProduction().isCommit()) {
                        cell.setCellStyle(bgOrange);
                    } else {
                        cell.setCellStyle(bgRed);
                    }
                }

                // Production - original container
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getProduction().getContainerOriginalVersion());

                // Production - new container
                cellIndex++;
                cell = row.createCell(cellIndex);
                cell.setCellValue(repository.getProduction().getContainerNewVersion());

                // Total change
                cellIndex++;
                cell = row.createCell(cellIndex);
                if (repository.isDone()) {
                    cell.setCellValue("Done");
                    cell.setCellStyle(bgGreen);
                }

                rowCounter++;
            }

            for (int i = 0; i <= row.getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            createDirectory(output.getParent());
            workbook.write(new FileOutputStream(output.toFile()));
            workbook.close();

        } catch (Exception e) {
            log.error("{}: {}", e.getClass().getName(), e.getMessage(), e);
        }
    }
}
