package de.geofabrik.sncf_railway_routing;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.util.Modules;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.GHServer;
import com.graphhopper.http.GraphHopperModule;
import com.graphhopper.http.GraphHopperServletModule;
import com.graphhopper.util.CmdArgs;

/**
 * Hello world!
 *
 */
public class RailwayRoutingMain {
    private RailwayHopper hopper;
    private CmdArgs commandline_args;
    
    public static void main( String[] args ) {
        new RailwayRoutingMain(CmdArgs.read(args));
    }
    
    private RailwayRoutingMain(CmdArgs args) {
        commandline_args = args;
        String action = commandline_args.get("action", "");
        hopper = new RailwayHopper(args);
        hopper.setGraphHopperLocation("./graph-cache");
        if (action.equals("import")) {
            importOSM();
        } else if (action.equals("web")) {
            web();
        }
    }
    
    private void importOSM() {
        hopper.importOrLoad();
        hopper.close();
    }
    
    private void web() {
        try {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    binder().requireExplicitBindings();

                    install(Modules.override(new GraphHopperModule(commandline_args)).with(new AbstractModule() {

                        @Override
                        protected void configure() {
                            bind(RailwayHopper.class).toInstance(hopper);
                        }

                        @Singleton
                        @Provides
                        protected GraphHopper createGraphHopper(CmdArgs args) {
                            return hopper;
                        }
                    }));

                    install(new GraphHopperServletModule(commandline_args));

                    bind(GuiceFilter.class);
                }
            });
            new GHServer(commandline_args).start(injector);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        // do not close as server is running in its own thread and needs open graphhopper
        // hopper.close();
    }
}
