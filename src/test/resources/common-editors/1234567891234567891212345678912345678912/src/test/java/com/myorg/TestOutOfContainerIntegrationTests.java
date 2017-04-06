package com.myorg;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = { "server.port=0", "spring.cloud.bus.enabled=false", "spring.cloud.discovery.enabled=false", "spring.cloud.consul.enabled=false" })
@WebAppConfiguration
public class TestOutOfContainerIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    public void canPerformSimpleGetRequestOnMicroservice() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().contentType("text/plain;charset=UTF-8")).andExpect(content().string("Hello REST Microservice World")).andReturn();
    }
}
