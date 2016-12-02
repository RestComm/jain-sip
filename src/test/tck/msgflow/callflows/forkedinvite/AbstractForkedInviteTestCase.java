/**
 *
 */
package test.tck.msgflow.callflows.forkedinvite;

import java.util.Hashtable;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import test.tck.msgflow.callflows.ScenarioHarness;

/**
 * @author M. Ranganathan
 *
 */
public class AbstractForkedInviteTestCase extends ScenarioHarness implements SipListener {

    private static final Logger LOG = LogManager.getLogger("test.tck");

    protected Shootist shootist;

    protected Shootme shootme;

    private Shootme shootme2;

    public AbstractForkedInviteTestCase() {

        super("forkedInviteTest", true);

        try {
            providerTable = new Hashtable();

        } catch (Exception ex) {
            LOG.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void setUp() {

        try {
            super.setUp(false,1,3);
            shootist = new Shootist(5060, 5070, super.getTiProtocolObjects(0));
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);

            this.shootme = new Shootme(5080, getTiProtocolObjects(1));
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);



            this.shootme2 = new Shootme(5090, getTiProtocolObjects(2));
            shootmeProvider = shootme2.createProvider();
            providerTable.put(shootmeProvider, shootme2);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            Proxy proxy = new Proxy(5070, getRiProtocolObjects(0));
            SipProvider provider = proxy.createSipProvider();
            provider.setAutomaticDialogSupportEnabled(false);
            providerTable.put(provider, proxy);
            provider.addSipListener(this);

            super.start();
        } catch (Exception ex) {
            System.out.println(ex.toString());
            fail("unexpected exception ");
        }
    }




    public void tearDown() {
        try {
            Thread.sleep(8000);
            this.shootist.checkState();
            this.shootme.checkState();
            this.shootme2.checkState();
            super.tearDown();
            Thread.sleep(2000);
            this.providerTable.clear();

            super.logTestCompleted();
        } catch (Exception ex) {
            LOG.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }



}
