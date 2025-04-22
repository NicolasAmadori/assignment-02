package main;

import models.User;
import services.UserService;
import utils.Logger;

public class App {
  public static void main(String[] args) {
    Logger.log("App started");
    User user = new User("Alice");
    UserService service = new UserService();
    service.process(user);
    SamePackage samePackage = new SamePackage();
    samePackage.samePackagePrint();
  }
}
