package de.charite.compbio.jannovar.reference;


/**
 * Helper code for the normalization of {@link GenomeChange}s.
 *
 * @author Manuel Holtgrewe <manuel.holtgrewe@charite.de>
 */
public final class GenomeChangeNormalizer {

	/**
	 * Transform a {@link GenomeChange} to its HGVS-normalized representation.
	 *
	 * @param transcript
	 *            the transcript with the sequence that should be used
	 * @param change
	 *            the genome change for which we want to return the HGVS-normalized representation for
	 * @param txPos
	 *            the corresponding position on the transcript
	 * @return normalized {@link GenomeChange}
	 */
	public static GenomeChange normalizeGenomeChange(TranscriptModel transcript, GenomeChange change,
			TranscriptPosition txPos) {
		switch (change.getType()) {
		case DELETION:
			return normalizeDeletion(transcript, change, txPos);
		case INSERTION:
			return normalizeInsertion(transcript, change, txPos);
		default:
			// TODO(holtgrem): Handle block substitution cse.
			return change;
		}
	}

	/**
	 * Transform an insertion {@link GenomeChange} to its HGVS-normalized representation.
	 *
	 * The algorithm works as follows. <code>String alt = change.getAlt()</code> is inserted into
	 * <code>transcript.sequence</code> <code>at the position <code>int pos = txPos.getPos()</code>. Then,
	 * <code>pos</code> is incremented as long as <code>pos + alt.length() &lt; transcript.sequence.length()</code> and
	 * <code>transcript.sequence[pos] = transcript.sequence[pos + alt.length()]</code>. The last <code>pos</code>
	 * fulfilling this condition is then used to construct the resulting {@link GenomeChange}.
	 *
	 * If necessary, the strand of <code>change</code> is set to the same as <code>transcript</code>.
	 *
	 * @param transcript
	 *            the transcript with the sequence that should be used
	 * @param change
	 *            the genome change for which we want to return the HGVS-normalized representation for
	 * @param txPos
	 *            the corresponding position on the transcript
	 * @return normalized {@link GenomeChange}
	 */
	public static GenomeChange normalizeInsertion(TranscriptModel transcript, GenomeChange change,
			TranscriptPosition txPos) {
		assert (change.getRef().length() == 0);
		if (change.getGenomePos().getStrand() != transcript.getStrand()) // ensure that we have the correct strand
			change = change.withStrand(transcript.getStrand());

		// Insert the ALT bases at the position indicated by txPos.
		int pos = txPos.getPos();
		StringBuilder builder = new StringBuilder(transcript.sequence);
		builder.insert(pos, change.getAlt());

		// Execute algorithm and compute the shift.
		int shift = 0;
		final int LEN = change.getAlt().length();
		final String seq = builder.toString();
		while ((pos + LEN < seq.length()) && (seq.charAt(pos) == seq.charAt(pos + LEN))) {
			++shift;
			++pos;
		}

		// Compute shifted transcript position and transform back to the genome position (we allow shifting over introns
		// since Mutalyzer does this).
		final TranscriptProjectionDecorator projector = new TranscriptProjectionDecorator(transcript);
		final GenomePosition shiftedPos;
		try {
			shiftedPos = projector.transcriptToGenomePos(txPos.shifted(shift));
		} catch (ProjectionException e) {
			throw new RuntimeException("Bug: transcript position must be valid here!", e);
		}

		if (shift == 0) // only rebuild if shift > 0
			return change;
		else
			return new GenomeChange(shiftedPos, "", seq.substring(pos, pos + LEN));
	}

	/**
	 * Transform a deletion {@link GenomeChange} into its HGVS-normalized representation.
	 *
	 * This simply works by shifting the interval to the left as long as the first deleted character equals the
	 * character after the deleetion.
	 *
	 * Note that this function should <b>only</b> be called if the change's deletion interval does not span a splice
	 * site.
	 *
	 * @param transcript
	 *            the transcript with the sequence that should be used
	 * @param change
	 *            the genome change for which we want to return the HGVS-normalized representation for
	 * @param txPos
	 *            the corresponding position on the transcript
	 * @return normalized {@link GenomeChange}
	 */
	public static GenomeChange normalizeDeletion(TranscriptModel transcript, GenomeChange change,
			TranscriptPosition txPos) {
		// TODO(holtgrem): check the splice site invariant?
		assert (change.getRef().length() != 0 && change.getAlt().length() == 0);
		if (change.getGenomePos().getStrand() != transcript.getStrand()) // ensure that we have the correct strand
			change = change.withStrand(transcript.getStrand());

		// Shift the deletion to the 3' (right) end of the transcript.
		int pos = txPos.getPos();
		final int LEN = change.getRef().length(); // length of the deletion
		final String seq = transcript.sequence;
		int shift = 0;

		while ((pos + LEN < seq.length()) && (seq.charAt(pos) == seq.charAt(pos + LEN))) {
			++shift;
			++pos;
		}

		if (shift == 0) // only rebuild if shift > 0
			return change;
		else
			return new GenomeChange(change.getGenomePos().shifted(shift), seq.substring(pos, pos + LEN), "");
	}

}
