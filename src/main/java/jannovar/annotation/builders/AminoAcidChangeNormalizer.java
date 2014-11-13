package jannovar.annotation.builders;

import jannovar.reference.AminoAcidChange;

/**
 * Helper for normalizing changes in amino acid sequences.
 *
 * @author Manuel Holtgrewe <manuel.holtgrewe@charite.de>
 */
public class AminoAcidChangeNormalizer {

	/**
	 * Normalize deletion {@link AminoAcidChange} for amino acid string
	 *
	 * @param ref
	 *            reference amino acid string to change
	 * @param change
	 *            the {@link AminoAcidChange} to normalize
	 * @return normalized AminoAcidChange
	 */
	public static AminoAcidChange normalizeDeletion(String ref, AminoAcidChange change) {
		if (change.ref.length() == 0 || change.alt.length() > 0)
			throw new Error("Invalid AminoAcidChange: " + change);

		// Compute shift of deletion.
		int shift = 0;
		final int LEN = change.ref.length();
		while (change.pos + LEN + shift < ref.length()
				&& ref.charAt(change.pos) == ref.charAt(change.pos + LEN + shift))
			shift += 1;
		if (shift == 0)
			return change;

		// Build new AminoAcidChange.
		StringBuilder changeRefBuilder = new StringBuilder();
		changeRefBuilder.append(change.ref.substring(shift, change.ref.length()));
		changeRefBuilder.append(ref.substring(change.pos + change.ref.length(), change.pos + change.ref.length()
				+ shift));
		return new AminoAcidChange(change.pos + shift, changeRefBuilder.toString(), "");
	}

}
