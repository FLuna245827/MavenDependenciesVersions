//package org.flunadela.depsvers;
//
//import org.eclipse.aether.DefaultRepositorySystemSession;
//import org.eclipse.aether.RepositorySystem;
//import org.eclipse.aether.RepositorySystemSession;
//import org.eclipse.aether.repository.LocalRepository;
//import org.eclipse.aether.repository.RemoteRepository;
//import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
//import org.eclipse.aether.supplier.RepositorySystemSupplier;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public final class Booter {
//    private static final Logger LOGGER = LoggerFactory.getLogger(Booter.class);
//
//    private Booter() {
//    }
//
//    /**
//     * Creates a RepositorySystem instance and registers the required services.
//     */
//    public static RepositorySystem newRepositorySystem() {
//        RepositorySystemSupplier repoSysSup = new RepositorySystemSupplier();
//        return repoSysSup.get();
//    }
//
//    /**
//     * Creates a RepositorySystemSession with a local repository (~/.m2/repository).
//     */
//    public static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
//        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
//
//        LocalRepository localRepo = new LocalRepository(getUserHomeM2Repository());
//        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
//
//        // Optional: Enable checksum, mirrors, proxies, etc.
//        session.setChecksumPolicy(CHECKSUM_POLICY_WARN);
//
//        return session;
//    }
//
//    /**
//     * Returns the default Maven local repository path.
//     */
//    private static String getUserHomeM2Repository() {
//        String home = System.getProperty("user.home");
//        return home + "/.m2/repository";
//    }
//
//    /**
//     * Convenience method: default Maven Central repository.
//     */
//    public static RemoteRepository newCentralRepository() {
//        return new RemoteRepository.Builder(
//                "central",
//                "default",
//                "https://repo.maven.apache.org/maven2/"
//        ).build();
//    }
//}