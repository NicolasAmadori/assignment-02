package nested.sub;

import java.util.List;
import models.Account;

public class DeepClass {
  private List<Account> accounts;

  public void setAccounts(List<Account> accs) {
    this.accounts = accs;
  }
}
