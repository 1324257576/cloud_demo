import com.demo.cloud.PaymentApp;
import com.demo.cloud.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author lqq
 * @date 2020/4/20
 */

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PaymentApp.class)
public class MapperScanTest {


    @Autowired
    SpringContextUtil springContextUtil;

    @Test
    public void test() {

        Object mapper = springContextUtil.applicationContext.getBean("simpleMapper");
        log.info("mapper={}", mapper);

    }
}
