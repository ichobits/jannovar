package de.charite.compbio.jannovar.vardbs.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import htsjdk.variant.variantcontext.VariantContext;

// TODO: implement overlap only behaviour and add as option

/**
 * Find matches between two allels (an observed and a database variant)
 * 
 * This class is an implementation detail and not part of the public interface.
 * 
 * @author Manuel Holtgrewe <manuel.holtgrewe@bihealth.de>
 */
public final class AlleleMatcher {

	/** Helper to use for indel normalization */
	final VariantNormalizer normalizer;

	/**
	 * Construct GenotypeMatcher
	 * 
	 * @param pathFasta
	 *            Path to FAI-indexed FASTA file
	 * @throws JannovarVarDBException
	 *             On problems with loading the FASTA/FAI file
	 */
	public AlleleMatcher(String pathFasta) throws JannovarVarDBException {
		this.normalizer = new VariantNormalizer(pathFasta);
	}

	/**
	 * Match genotypes of two {@link VariantContext}s
	 * 
	 * Indels will be left-shifted and normalized when necessary
	 * 
	 * @param obsVC
	 *            {@link VariantContext} describing the observed variant
	 * @param dbVC
	 *            {@link VariantContext} describing the database variant
	 * @return {@link Collection} of {@link GenotypeMatch}es for the two variants
	 */
	public Collection<GenotypeMatch> matchGenotypes(VariantContext obsVC, VariantContext dbVC) {
		List<GenotypeMatch> result = new ArrayList<>();

		// Get normalized description of all alternative observed and database alleles
		Collection<VariantDescription> obsVars = ctxToVariants(obsVC);
		Collection<VariantDescription> dbVars = ctxToVariants(dbVC);

		int i = 1;  // excludes reference allele
		for (VariantDescription obsVar : obsVars) {
			int j = 1;  // excludes reference allele
			for (VariantDescription dbVar : dbVars) {
				if (dbVar.equals(obsVar))
					result.add(new GenotypeMatch(i, j, obsVC, dbVC));
				j += 1;
			}
			
			i += 1;
		}

		return result;
	}

	Collection<VariantDescription> ctxToVariants(VariantContext vc) {
		List<VariantDescription> vars = new ArrayList<>();
		for (int i = 1; i < vc.getNAlleles(); ++i) {
			vars.add(new VariantDescription(vc.getContig(), vc.getStart() - 1, vc.getAlleles().get(0).getBaseString(),
					vc.getAlleles().get(i).getBaseString()));
		}
		return vars;
	}

}
