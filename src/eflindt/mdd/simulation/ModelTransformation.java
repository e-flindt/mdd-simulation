package eflindt.mdd.simulation;

import java.util.function.Consumer;

import eflindt.mdd.simulation.Artifact.ArtifactVersion;

/**
 * A sub class of {@link Artifact} representing transforming actions over an
 * {@link Artifact}.
 * 
 * @author Eric Flindt
 *
 */
public interface ModelTransformation extends Artifact, Consumer<ArtifactVersion> {

	/**
	 * Only needed for the copy mechanism.
	 */
	Consumer<ArtifactVersion> getTransformation();
	
}