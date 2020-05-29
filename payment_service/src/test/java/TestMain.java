import com.demo.cloud.PaymentApp;
import com.demo.cloud.model.Payment;
import com.demo.cloud.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author lqq
 * @date 2020/4/3
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PaymentApp.class)
@Slf4j
public class TestMain {


    @Autowired
    PaymentService paymentService;


    @Autowired
    private WebApplicationContext context;

    @Getter
    private MockMvc mvc;


    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();  //构造MockMvc
    }


    @Test
    public void test3() throws Exception {

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSerial("test_data_10");
        String url = "http://localhost:8001/update";

        MvcResult mvcResult =
                this.getMvc().perform(MockMvcRequestBuilders.put(url)

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                        .andExpect(status().isOk())
                        .andDo(print()).andReturn();
    }

    @Test
    public void test0() {
        Payment payment = paymentService.findById(1L);
        log.info("r:{}", payment);
    }

    @Test
    public void test1() {
        Payment payment = new Payment();
        payment.setSerial("test_data_1");
        int r = paymentService.save(payment);
        log.info("r:{}{}", r, payment);
    }

    @Test
    public void test2() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSerial("test_data_0");

        int r = paymentService.update(payment);
        log.info("r:{}{}", r, payment);
    }
}
