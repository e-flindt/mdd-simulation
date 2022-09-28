package eflindt.mdd.simulation;

import java.util.Set;

import eflindt.mdd.simulation.Artifact.ArtifactVersion;

/**
 * This class represents a version control system that provides queries over
 * model relationships in addition to push / pull.
 * 
 * These queries are defined such that only version information is returned,
 * signaling that the workflow can make due without the actual artifact
 * 
 * @author Eric Flindt
 *
 */
public interface Repository {

	Artifact pull(ArtifactVersion version);

	Set<ArtifactVersion> getInstances(ArtifactVersion version);

	Set<ArtifactVersion> getMetamodels(ArtifactVersion version);

	Set<ArtifactVersion> getInputs(ArtifactVersion version);

	Set<ArtifactVersion> getTransformations(ArtifactVersion version);

	Set<ArtifactVersion> getConsumers(ArtifactVersion version);

	void push(Artifact a);

	void push(Artifact... a);

}