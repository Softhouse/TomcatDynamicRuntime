package org.tdr.bootstrap;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class OSGiContextListener implements LifecycleListener {

	static private Log log = LogFactory.getLog(OSGiContextListener.class);
	
	private OSGiFileDirContext dirContext;
	
	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if ( event.getType().equals(Lifecycle.AFTER_START_EVENT) ) {
			OSGiContext context = (OSGiContext) event.getSource(); 
			
			// TODO: Inject the servlet context into runtime appl instance
			
			// TODO: It seems that is not possible to add servlets after the context has been initalized. WHY????
			// -> Då är vi tillbaka till en magisk proxy servlet som lyssnar på /* och krockar med JSF!!
			
			// TODO: MÅSTE FINNAS NÅGOT SÄKRARE SÄTT ÄN ATT GÖRA SÅ HÄR????
			
			// DEPRICATED!! Could be handled by the context class itself!!
			
			this.dirContext = (OSGiFileDirContext) TLSReference.get("osgi/dirContext");
			if ( this.dirContext != null && this.dirContext.getRuntimeApplication() != null ) {
				this.dirContext.getRuntimeApplication().initializeContext(context);
			}
		}
		else if ( event.getType().equals(Lifecycle.BEFORE_DESTROY_EVENT) ) {
			log.info("Destroying context...");
			this.dirContext.getRuntimeApplication().destroyContext();
		}
		
	}

}
