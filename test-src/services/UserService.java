package services;

import models.User;
import main.utils.Logger;

public class UserService {
  public void process(User user) {
    Logger.log("Processing user: " + user.getName());
  }
}
