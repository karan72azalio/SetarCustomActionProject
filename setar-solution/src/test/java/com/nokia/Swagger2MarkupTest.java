package com.nokia;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nokia.nsw.uiv.setar.config.ModelConfiguration;
import com.nokia.nsw.uiv.TestConfiguration;
import com.nokia.nsw.uiv.runtime.config.SwaggerConfiguration;
import com.nokia.nsw.uiv.runtime.config.CoreConfiguration;
//import io.github.robwin.swagger2markup.SwaggerConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureRestDocs(outputDir = "build/asciidoc/snippets")
@SpringBootTest(classes = {TestConfiguration.class,ModelConfiguration.class,CoreConfiguration.class,SwaggerConfiguration.class})
@AutoConfigureMockMvc
public class Swagger2MarkupTest {

    private static final Logger LOG = LoggerFactory.getLogger(Swagger2MarkupTest.class);


    @Autowired
    private MockMvc mockMvc;


    @Test
    public void createSpringfoxSwaggerJson() throws Exception {
        String outputDir = System.getProperty("io.springfox.staticdocs.outputDir");
        if (outputDir == null || outputDir.isEmpty()) {
            log.warn("io.springfox.staticdocs.outputDir property empty: Skipping test");
            return;
        }
        MvcResult mvcResult = this.mockMvc.perform(get("/v2/api-docs?group=Action API")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        String swaggerJson = response.getContentAsString();
        Files.createDirectories(Paths.get(outputDir));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputDir, "swagger.json"), StandardCharsets.UTF_8)){
            writer.write(swaggerJson);
        }
    }
}
