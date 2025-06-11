package com.github.ronlievens.regov.task.rewrite.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.shell.model.AzureRepository;
import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.util.MapperUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.TreeSet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.apache.commons.csv.QuoteMode.MINIMAL;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CsvUtils {

    private static final String CSV_FIELD_SEPARATOR = ";";

    private static final String RESULT_FILE_SEARCH = "search-result-list.csv";
    private static final String RESULT_FILE_JOB = "job_rewrite_%s_part_%s.csv";

    private static final ObjectMapper MAPPER = MapperUtils.createJsonMapper();

    public static void loadSearchResultFromCsv(@NonNull final RewriteContext rewriteContext) throws IOException {
        rewriteContext.setRepositories(new TreeSet<>());
        try (val reader = Files.newBufferedReader(rewriteContext.getBatchFile(), UTF_8)) {
            // Skip headers
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(CSV_FIELD_SEPARATOR);
                if (values.length >= 5) {
                    log.info("Loading repository: {}/{}/{}", values[0], values[1], values[2]);
                    val originalBytes = Base64.getDecoder().decode(values[4]);
                    val originalText = new String(originalBytes, UTF_8);
                    rewriteContext.getRepositories().add(MAPPER.readValue(originalText, AzureRepository.class));
                }
            }
        }
    }

    public static void writeSearchResultToCsv(@NonNull final RewriteContext rewriteContext) throws IOException {
        var index = 0;
        var partIndex = 1;
        val filename = (rewriteContext.getNumberRows() == null) ? RESULT_FILE_SEARCH : RESULT_FILE_JOB;
        BufferedWriter writer = null;
        CSVPrinter printer = null;

        val csvFormat = EXCEL
            .builder()
            .setDelimiter(';')
            .setQuoteMode(MINIMAL)
            .setHeader("organization", "project", "name", "url", "data")
            .get();

        for (val repository : rewriteContext.getRepositories()) {

            if (writer == null) {
                val csvFile = rewriteContext.getDestination().resolve(filename.formatted((rewriteContext.getTicket() != null) ? rewriteContext.getTicket() : "", partIndex));
                writer = Files.newBufferedWriter(csvFile, UTF_8, CREATE, TRUNCATE_EXISTING);

                // Write BOM character for Excel
                writer.write('\ufeff');

                printer = new CSVPrinter(writer, csvFormat);
            }

            val originalText = MAPPER.writeValueAsString(repository);
            val base64Text = Base64.getEncoder().encodeToString(originalText.getBytes(UTF_8));

            printer.printRecord(
                repository.getProject().getOrganizationName(),
                repository.getProject().getName(),
                repository.getName(),
                repository.getWebUrl().toString(),
                base64Text);
            index++;

            if (rewriteContext.getNumberRows() != null && index >= rewriteContext.getNumberRows()) {
                printer.flush();
                writer.close();
                writer = null;
                partIndex++;
                index = 0;
            }
        }

        if (writer != null) {
            printer.flush();
            writer.close();
        }
    }
}
