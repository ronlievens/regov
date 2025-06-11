package com.github.ronlievens.regov.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.task.config.model.AzureOrganizationSettingsModel;
import com.github.ronlievens.regov.task.config.model.SettingModel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.github.ronlievens.regov.util.MapperUtils.createJsonMapper;
import static com.github.ronlievens.regov.util.PathAssertUtils.readFileAsStringFromClasspath;
import static com.github.ronlievens.regov.util.PathAssertUtils.writeToFileInTargetDirectory;
import static com.github.ronlievens.regov.util.PathUtils.findFileInClasspath;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

@Slf4j
public class PropertyUtilsTest {

    private static final ObjectMapper MAPPER = createJsonMapper(false, false);

    @Test
    public void test_structure() throws Exception {
        val actual = new SettingModel();

        val a = new AzureOrganizationSettingsModel();
        a.getProjects().add("AA");
        a.getProjects().add("AB");
        actual.getAzure().getOrganizations().put("A", a);

        val b = new AzureOrganizationSettingsModel();
        b.getProjects().add("AA");
        b.getProjects().add("AB");
        actual.getAzure().getOrganizations().put("B", b);

        val actualString = MAPPER.writeValueAsString(actual);
        writeToFileInTargetDirectory(actualString, "./config/structure.json");
        val expectedString = readFileAsStringFromClasspath("./config/structure.json");
        JSONAssert.assertEquals(expectedString, actualString, LENIENT);
    }


    @Test
    public void test_load_profile_one() throws Exception {
        val actual = PropertyUtils.load("one-actual", findFileInClasspath("./config"));
        val actualString = MAPPER.writeValueAsString(actual);
        writeToFileInTargetDirectory(actualString, "./config/one-actual.json");
        val expectedString = readFileAsStringFromClasspath("./config/one-expected.json");
        JSONAssert.assertEquals(expectedString, actualString, STRICT);
    }

    @Test
    public void test_load_profile_two() throws Exception {
        val actual = PropertyUtils.load("two-actual", findFileInClasspath("./config"));
        val actualString = MAPPER.writeValueAsString(actual);
        writeToFileInTargetDirectory(actualString, "./config/two-actual.json");
        val expectedString = readFileAsStringFromClasspath("./config/two-expected.json");
        JSONAssert.assertEquals(expectedString, actualString, STRICT);
    }

}
