package test.tck.msgflow.callflows;

public class AssertUntil {
    private static final long POLLING_FREQ = 500; 
    public static boolean assertUntil(TestAssertion assertion, long millis) throws InterruptedException{
        long timeout = 0;
        while (timeout < millis) {
            if (assertion.assertCondition()) {
                return true;
            }
            Thread.sleep(POLLING_FREQ);
            timeout = timeout + POLLING_FREQ;
        }
        return false;        
    }
}
