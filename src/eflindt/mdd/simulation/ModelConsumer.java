package eflindt.mdd.simulation;

import java.util.function.Predicate;

import eflindt.mdd.simulation.Artifact.ArtifactVersion;

/**
 * A sub class of {@link Artifact} representing consuming actions over an
 * {@link Artifact}.
 * 
 * @author Eric Flindt
 *
 */
public interface ModelConsumer extends Artifact, Predicate<ArtifactVersion> {

	/**
	 * Only needed for the copy mechanism.
	 */
	Predicate<ArtifactVersion> getConsumer();

}