//package org.flunadela.depsvers;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import org.apache.maven.model.Dependency;
//import org.apache.maven.model.Model;
//import org.apache.maven.model.Parent;
//import org.apache.maven.model.Repository;
//import org.apache.maven.model.building.DefaultModelBuilder;
//import org.apache.maven.model.building.DefaultModelBuilderFactory;
//import org.apache.maven.model.building.DefaultModelBuildingRequest;
//import org.apache.maven.model.building.FileModelSource;
//import org.apache.maven.model.building.ModelBuildingException;
//import org.apache.maven.model.building.ModelBuildingRequest;
//import org.apache.maven.model.building.ModelBuildingResult;
//import org.apache.maven.model.building.ModelSource2;
//import org.apache.maven.model.resolution.InvalidRepositoryException;
//import org.apache.maven.model.resolution.ModelResolver;
//import org.apache.maven.model.resolution.UnresolvableModelException;
//import org.eclipse.aether.RepositorySystem;
//import org.eclipse.aether.RepositorySystemSession;
//import org.eclipse.aether.artifact.Artifact;
//import org.eclipse.aether.artifact.DefaultArtifact;
//import org.eclipse.aether.repository.RemoteRepository;
//import org.eclipse.aether.resolution.ArtifactRequest;
//import org.eclipse.aether.resolution.ArtifactResult;
//
//public class EffectiveModel {
//
//    public static Model getEffectiveModel(File xmlPomFile) throws ModelBuildingException {
//        DefaultModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
//
//        ModelBuildingRequest req = new DefaultModelBuildingRequest();
//        req.setPomFile(xmlPomFile);
//
//        RepositorySystem repoSystem = Booter.newRepositorySystem();
//        RepositorySystemSession session = Booter.newRepositorySystemSession(repoSystem);
//
//        List<RemoteRepository> repos = new ArrayList<>();
//        repos.add(Booter.newCentralRepository());
//
//        req.setModelResolver(new MyModelResolver(repoSystem, session, repos));
//
//        ModelBuildingResult result = builder.build(req);
//
//        return result.getEffectiveModel();
//    }
//
//    static class MyModelResolver implements ModelResolver {
//
//        private final RepositorySystem repoSystem;
//        private final RepositorySystemSession repoSession;
//        private final List<RemoteRepository> repositories;
//
//        public MyModelResolver(RepositorySystem repoSystem,
//                               RepositorySystemSession repoSession,
//                               List<RemoteRepository> repositories) {
//            this.repoSystem = repoSystem;
//            this.repoSession = repoSession;
//            this.repositories = new ArrayList<>(repositories);
//        }
//
//        @Override
//        public ModelResolver newCopy() {
//            return new MyModelResolver(repoSystem, repoSession, repositories);
//        }
//
//        @Override
//        public void addRepository(Repository repository) throws InvalidRepositoryException {
//            RemoteRepository remoteRepo = new RemoteRepository.Builder(
//                    repository.getId(),
//                    "default",
//                    repository.getUrl()
//            ).build();
//
//            repositories.add(remoteRepo);
//        }
//
//        @Override
//        public ModelSource2 resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
//            try {
//                Artifact pomArtifact = new DefaultArtifact(
//                        groupId, artifactId, "pom", version
//                );
//
//                ArtifactRequest request = new ArtifactRequest()
//                        .setArtifact(pomArtifact)
//                        .setRepositories(repositories);
//
//                ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
//
//                File fileFullPath = result.getArtifact().getFile();
//                return new FileModelSource(fileFullPath);
//
//            } catch (Exception e) {
//                throw new UnresolvableModelException(
//                        e.getMessage(), groupId, artifactId, version, e
//                );
//            }
//        }
//
//        @Override
//        public ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
//            return resolveModel(
//                    parent.getGroupId(),
//                    parent.getArtifactId(),
//                    parent.getVersion()
//            );
//        }
//
//        @Override
//        public ModelSource2 resolveModel(Dependency dependency) throws UnresolvableModelException {
//            return resolveModel(
//                    dependency.getGroupId(),
//                    dependency.getArtifactId(),
//                    dependency.getVersion()
//            );
//        }
//
////        public ModelSource resolveModel(Artifact artifact) throws UnresolvableModelException {
////            return resolveModel(
////                    artifact.getGroupId(),
////                    artifact.getArtifactId(),
////                    artifact.getVersion()
////            );
////        }
//
//        @Override
//        public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {
//            addRepository(repository);
//        }
//    }
//}
