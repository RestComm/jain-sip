package gov.nist.javax.sip;

/**
 * All tasks that requires to be under ConcurrenyControlMode have to extend
 * this interface so they can be assigned to proper thread.
 * 
 * The object hash will be used by the thread pool to consistently assign all
 * tasks to the same thread.
 * 
 * The afinnity object will be added to MDC vars automatically behind the scenes.
 * 
 */
public interface ThreadAffinityTask extends Runnable, ThreadAffinityIdentifier{

}
