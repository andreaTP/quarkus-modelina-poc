package io.modelina.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusModelinaResourceTest {
    @Test
    public void testHelloEndpoint() {
        given().when()
                .get("/quarkus-modelina")
                .then()
                .statusCode(200)
                .body(is("{\"email\":\"my-dummy-email@modelina.com\"}"));
    }
}
