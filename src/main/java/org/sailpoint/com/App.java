package org.sailpoint.com;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.api.SailPointContext;
import sailpoint.connector.JDBCConnector;
import sailpoint.object.Application;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.object.ProvisioningPlan.PermissionRequest;
import sailpoint.object.ProvisioningResult;
import sailpoint.object.Schema;
import sailpoint.tools.GeneralException;


/**
 * Hello world!
 */
public class App {

    public static Log log = LogFactory.getLog(App.class);

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }


    public String getAttributeRequestValue(AccountRequest acctReq, String attribute) {

        if (acctReq != null) {
            AttributeRequest attrReq = acctReq.getAttributeRequest(attribute);
            if (attrReq != null) {
                return (String) attrReq.getValue();
            }
        }
        return null;
    }

    public boolean checkExistAttributeValueOnAccountRequest(AccountRequest acctReq, String attribute) {
        if (getAttributeRequestValue(acctReq, attribute) == null) {
            log.info(attribute + " is not included in the AccountRequest");
            return false;
        }
        return true;
    }

    public ProvisioningResult processProvisioning(Connection connection, ProvisioningPlan plan, Application application, Schema schema) throws GeneralException {
        {

            log.debug("processProvisioning [start]");

            ProvisioningResult result = new ProvisioningResult();
            if (plan != null) {

                List<AccountRequest> accounts = plan.getAccountRequests();

                log.info("accounts.size()=" + accounts.size());

                if ((accounts != null) && (accounts.size() > 0)) {
                    for (AccountRequest account : accounts) {

                        PreparedStatement statement = null;
                        try {

                            if (AccountRequest.Operation.Create.equals(account.getOperation())) {
                                // Ideally we should first check to see if the account already exists.
                                // As written, this just assumes it does not.

                                log.info("AccountRequest.Operation.Create: " + (String) account.getNativeIdentity());


                                statement = connection.prepareStatement("insert into users (user_id,email,group_name) values (?,?,?)");
                                statement.setString(1, (String) account.getNativeIdentity());
                                statement.setString(2, getAttributeRequestValue(account, getEmail()));
                                statement.setString(3, getAttributeRequestValue(account, getGroupName()));
                                statement.executeUpdate();

                                result.setStatus(ProvisioningResult.STATUS_COMMITTED);

                            } else if (AccountRequest.Operation.Modify.equals(account.getOperation())) {

                                // Modify account request -- change email

                                log.info("AccountRequest.Operation.Modify: " + (String) account.getNativeIdentity());

                                log.info((checkExistAttributeValueOnAccountRequest(account, getGroupName())));
                                log.info((checkExistAttributeValueOnAccountRequest(account, getEmail())));

                                statement = connection.prepareStatement("update users set email = ? where user_id = ?");
                                statement.setString(2, (String) account.getNativeIdentity());
                                if (account != null) {
                                    AttributeRequest attrReq = account.getAttributeRequest(getEmail());
                                    if (attrReq != null && ProvisioningPlan.Operation.Remove.equals(attrReq.getOperation())) {
                                        statement.setNull(1, Types.NULL);
                                        statement.executeUpdate();
                                    } else {
                                        statement.setString(1, (String) attrReq.getValue());
                                        statement.executeUpdate();
                                    }
                                }
                                result.setStatus(ProvisioningResult.STATUS_COMMITTED);

                            } else if (AccountRequest.Operation.Delete.equals(account.getOperation())) {

                                statement = connection.prepareStatement((String) application.getAttributeValue("account.deleteSQL"));

                                statement.setString(1, (String) account.getNativeIdentity());
                                statement.executeUpdate();

                                result.setStatus(ProvisioningResult.STATUS_COMMITTED);

                            } else if (AccountRequest.Operation.Disable.equals(account.getOperation())) {

                                // Disable, not supported.

                            } else if (AccountRequest.Operation.Enable.equals(account.getOperation())) {

                                // Enable, not supported.

                            } else if (AccountRequest.Operation.Lock.equals(account.getOperation())) {

                                // Lock, not supported.

                            } else if (AccountRequest.Operation.Unlock.equals(account.getOperation())) {

                                // Unlock, not supported.

                            } else {
                                // Unknown operation!
                            }
                        } catch (SQLException e) {
                            result.setStatus(ProvisioningResult.STATUS_FAILED);
                            result.addError(e);
                        } finally {
                            if (statement != null) {
                                try {
                                    statement.close();
                                } catch (SQLException e) {
                                    // ignore - throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }

            log.debug("processProvisioning [end]");
            return result;
        }

    }

    private static String getEmail() {
        return "email";
    }

    private static String getGroupName() {
        return "group_name";
    }
}

