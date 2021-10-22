import com.stackabuse.SpringBootQuartzApplication;
import com.stackabuse.entity.SchedulerJobInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.util.List;

@SpringBootTest(classes = SpringBootQuartzApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void test(){
        ResponseEntity<String> executeResponse = restTemplate.postForEntity("http://localhost:"+port+"/api/saveOrUpdate?jobName=batch-job&jobGroup=sisal&jobClass=batch-job&cronExpression=0/15 * * * * ?", null, String.class);

        ResponseEntity<List<SchedulerJobInfo>> res = restTemplate.getForEntity("http://localhost:"+port+"/api/getAllJobs", null, String.class);

        Assertions.assertNotNull(res);
    }
}
