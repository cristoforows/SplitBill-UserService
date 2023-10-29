package SplitBill.UserService.UserService;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() {
    // Create a Router
    vertx.deployVerticle(new FindUserVerticle());
    vertx.deployVerticle(new UserDetailsVerticle());

    Router router = Router.router(vertx);

    router.get("/api/v1/email/:email").handler(this::FindUserByEmailHandler);
//    router.get("/api/v1/user/:id").handler(this::FindUserByIdHandler);
//    router.get("/api/v1/phonenumber/:phoneNumber").handler(this::FindExistingPhoneNumberHandler);
    router.post("/api/v1/user")
      .consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::CreateUserHandler);


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8989)
      .onSuccess(server ->
        System.out.println(
          "HTTP server started on port " + server.actualPort()
        )
      );
  }

  private void CreateUserHandler(RoutingContext routingContext) {

    String payload = routingContext.body().asString();
    vertx.eventBus().request("createUser.handler.addr", payload, reply -> {
      if (reply.succeeded()) {
        routingContext.response().setStatusCode(200).end(reply.result().body().toString());
      } else {

        System.out.println(reply.cause());
        System.out.println("IUFHUIASHUIOHASUIOHASDUIOHASDUIOHASDUIOUIOASDUIOHASDUIOHASD");
        routingContext.response().setStatusCode(500).end("User not created");
      }
    });
  }
//
//  private void FindExistingPhoneNumberHandler(RoutingContext routingContext) {
//
//  }
//
//  private void FindUserByIdHandler(RoutingContext routingContext) {
//
//  }

  private void FindUserByEmailHandler(RoutingContext routingContext) {
    vertx.eventBus().request("findEmail.handler.addr", routingContext.request().getParam("email"), reply -> {
      if (reply.succeeded()) {
        routingContext.response().setStatusCode(200).end(reply.result().body().toString());
      } else {
        routingContext.response().setStatusCode(404).end("User not found");
      }
    });
  }
}
