package eu.excitementproject.eop.biutee.rteflow.macro.gap.pastabased;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.excitementproject.eop.biutee.rteflow.macro.gap.GapException;
import eu.excitementproject.eop.biutee.utilities.BiuteeConstants;
import eu.excitementproject.eop.common.datastructures.SimpleValueSetMap;
import eu.excitementproject.eop.common.datastructures.ValueSetMap;
import eu.excitementproject.eop.common.datastructures.immutable.ImmutableList;
import eu.excitementproject.eop.common.datastructures.immutable.ImmutableSet;
import eu.excitementproject.eop.common.representation.parse.representation.basic.Info;
import eu.excitementproject.eop.common.representation.parse.representation.basic.InfoGetFields;
import eu.excitementproject.eop.common.representation.parse.tree.AbstractNode;
import eu.excitementproject.eop.common.representation.parse.tree.TreeAndParentMap;
import eu.excitementproject.eop.common.representation.parse.tree.TreeIterator;
import eu.excitementproject.eop.common.representation.pasta.Predicate;
import eu.excitementproject.eop.common.representation.pasta.PredicateArgumentStructure;
import eu.excitementproject.eop.common.representation.pasta.TypedArgument;
import eu.excitementproject.eop.common.utilities.Utils;
import eu.excitementproject.eop.transformations.utilities.InfoObservations;
import eu.excitementproject.eop.transformations.utilities.parsetreeutils.TreeUtilities;

/**
 * 
 * @author Asher Stern
 * @since Aug 20, 2013
 *
 * @param <I>
 * @param <S>
 */
public class PastaGapFeaturesV3Calculator<I extends Info, S extends AbstractNode<I, S>>
{
	public PastaGapFeaturesV3Calculator(TreeAndParentMap<I, S> hypothesisTree,
			Set<PredicateArgumentStructure<I, S>> hypothesisStructures,
			TreeAndParentMap<I, S> textTree,
			Set<PredicateArgumentStructure<I, S>> textStructures,
			ImmutableSet<String> stopWords)
	{
		super();
		this.hypothesisTree = hypothesisTree;
		this.hypothesisStructures = hypothesisStructures;
		this.textTree = textTree;
		this.textStructures = textStructures;
		this.stopWords = stopWords;
	}
	
	
	public void calculate() throws GapException
	{
		buildContentLemmasOfHypothesis();
		buildLemmasOfText();
		buildArgumentMap();
		calculateLists();
	}
	
	
	
	
	public List<PredicateAndArgument<I, S>> getCalculatedNoMatchNamedEntities()
	{
		return calculatedNoMatchNamedEntities;
	}


	public List<PredicateAndArgument<I, S>> getCalculatedNoMatch()
	{
		return calculatedNoMatch;
	}


	public List<PredicateAndArgument<I, S>> getCalculatedMatchWrongPredicateMissingWords()
	{
		return calculatedMatchWrongPredicateMissingWords;
	}


	public List<PredicateAndArgument<I, S>> getCalculatedMatchWrongPredicate()
	{
		return calculatedMatchWrongPredicate;
	}


	public List<PredicateAndArgument<I, S>> getCalculatedMatchMissingWords()
	{
		return calculatedMatchMissingWords;
	}


	public List<PredicateAndArgument<I, S>> getCalculatedMatch()
	{
		return calculatedMatch;
	}

	public Set<String> getCalculatedTotallyOmittedHypothesisContentLemmasNonPredicates()
	{
		return calculatedTotallyOmittedHypothesisContentLemmasNonPredicates;
	}


	public Set<String> getCalculatedTotallyOmittedHypothesisContentLemmasPredicates()
	{
		return calculatedTotallyOmittedHypothesisContentLemmasPredicates;
	}
	

	
	//////////////////// PRIVATE ////////////////////



	private void buildContentLemmasOfHypothesis()
	{
		Set<S> hypothesisPredicateNodes = getPredicateNodes(hypothesisStructures);
		contentLemmasOfHypothesisNonPredicates_lowerCase = contentLemmasOfNodes_lowerCase(TreeIterator.iterableTree(hypothesisTree.getTree()),hypothesisPredicateNodes);
		contentLemmasOfHypothesisPredicates_lowerCase = contentLemmasOfNodes_lowerCase(hypothesisPredicateNodes);
	}
	
	private void buildLemmasOfText()
	{
		lemmasOfText_lowerCase = new LinkedHashSet<>();
		for (S node : TreeIterator.iterableTree(textTree.getTree()))
		{
			lemmasOfText_lowerCase.add(InfoGetFields.getLemma(node.getInfo()).toLowerCase());
		}
		
		for (PredicateArgumentStructure<I, S> textStructure : textStructures)
		{
			ImmutableList<String> verbalForms = textStructure.getPredicate().getVerbsForNominal();
			if (verbalForms!=null)
			{
				for (String verbal : verbalForms)
				{
					lemmasOfText_lowerCase.add(verbal);
				}
			}
		}
	}
	
	private void buildArgumentMap()
	{
		mapArgumentsHypothesisToText = new SimpleValueSetMap<>();
		hypothesisArguments = listOfArguments(hypothesisStructures);
		textArguments = listOfArguments(textStructures);
		
		for (PredicateAndArgument<I, S> hypothesisArgument : hypothesisArguments)
		{
			for (PredicateAndArgument<I, S> textArgument : textArguments)
			{
				if (argumentsMatch(textArgument,hypothesisArgument))
				{
					mapArgumentsHypothesisToText.put(hypothesisArgument, textArgument);	
				}
			}
		}
	}
	
	private boolean argumentsMatch(PredicateAndArgument<I, S> textArgument, PredicateAndArgument<I, S> hypothesisArgument)
	{
		String hypothesisLemma = InfoGetFields.getLemma(hypothesisArgument.getArgument().getArgument().getSemanticHead().getInfo()).toLowerCase();
		if (BiuteeConstants.PASTA_GAP_STRICT_ARGUMENT_HEAD_MODE)
		{
			String textLemma = InfoGetFields.getLemma(textArgument.getArgument().getArgument().getSemanticHead().getInfo()).toLowerCase();
			return textLemma.equals(hypothesisLemma);
		}
		else
		{
			return (TreeUtilities.lemmasLowerCaseOfNodes(textArgument.getArgument().getArgument().getNodes()).contains(hypothesisLemma));
		}
	}
	
	private List<PredicateAndArgument<I, S>> listOfArguments(Set<PredicateArgumentStructure<I, S>> structures)
	{
		List<PredicateAndArgument<I, S>> ret = new LinkedList<>();
		for (PredicateArgumentStructure<I, S> structure : structures)
		{
			for (TypedArgument<I, S> argument : structure.getArguments())
			{
				ret.add(new PredicateAndArgument<I,S>(structure, argument));
			}
		}
		return ret;
	}
	
	
	
	private void calculateLists()
	{
		calculatedNoMatchNamedEntities = new LinkedList<>();
		calculatedNoMatch = new LinkedList<>();
		calculatedMatchWrongPredicateMissingWords = new LinkedList<>();
		calculatedMatchWrongPredicate = new LinkedList<>();
		calculatedMatchMissingWords = new LinkedList<>();
		calculatedMatch = new LinkedList<>();

		for (PredicateAndArgument<I, S> hypothesisArgument : hypothesisArguments)
		{
			//if (!mapArgumentsHypothesisToText.containsKey(hypothesisArgument))

			String hypothesisArgumentLemma_lowerCase = InfoGetFields.getLemma(hypothesisArgument.getArgument().getArgument().getSemanticHead().getInfo()).toLowerCase();
			if (!lemmasOfText_lowerCase.contains(hypothesisArgumentLemma_lowerCase))
			{
				boolean namedEntity = (InfoGetFields.getNamedEntityAnnotation(hypothesisArgument.getArgument().getArgument().getSemanticHead().getInfo())!=null);
				if (namedEntity)
				{
					calculatedNoMatchNamedEntities.add(hypothesisArgument);
				}
				else
				{
					calculatedNoMatch.add(hypothesisArgument);
				}
			}
			else
			{
				boolean predicateOK = false;
				boolean wordsOK = false;
				
				Set<String> hypothesisArgumentContentLemmas_lowerCase = contentLemmasOfArgument_lowerCase(hypothesisArgument.getArgument());
				
				if (mapArgumentsHypothesisToText.containsKey(hypothesisArgument))
				{
					for (PredicateAndArgument<I, S> textArgument : mapArgumentsHypothesisToText.get(hypothesisArgument))
					{
						if (!predicateOK) {if (samePredicate(hypothesisArgument, textArgument))
						{
							predicateOK=true;
						}}
						if (!wordsOK) { if(  allLemmasOfArgument_lowerCase(textArgument.getArgument()).containsAll(hypothesisArgumentContentLemmas_lowerCase)  )
						{
							wordsOK=true;
						}}
					}
				}
				
				if (predicateOK&&wordsOK) {calculatedMatch.add(hypothesisArgument);}
				else if ((!predicateOK)&&wordsOK) {calculatedMatchWrongPredicate.add(hypothesisArgument);}
				else if (predicateOK&&(!wordsOK)) {calculatedMatchMissingWords.add(hypothesisArgument);}
				else if ((!predicateOK)&&(!wordsOK)) {calculatedMatchWrongPredicateMissingWords.add(hypothesisArgument);}
			}
		}
		
//		private Set<String> calculatedTotallyOmittedHypothesisContentLemmasNonPredicates;
//		private Set<String> calculatedTotallyOmittedHypothesisContentLemmasPredicates;

		calculatedTotallyOmittedHypothesisContentLemmasNonPredicates = notIncludedInText(contentLemmasOfHypothesisNonPredicates_lowerCase);
		calculatedTotallyOmittedHypothesisContentLemmasPredicates = notIncludedInText(contentLemmasOfHypothesisPredicates_lowerCase);
	}
	
	private Set<String> notIncludedInText(Set<String> _hypothesisLemmas_lowerCase)
	{
		Set<String> ret = new LinkedHashSet<>();
		for (String contentLemma : _hypothesisLemmas_lowerCase)
		{
			if (!lemmasOfText_lowerCase.contains(contentLemma))
			{
				ret.add(contentLemma);
			}
		}
		return ret;
	}
	
	
	
	private boolean samePredicate(PredicateAndArgument<I, S> hypothesisArgument, PredicateAndArgument<I, S> textArgument)
	{
		return (Utils.intersect(
				predicateLemmasLowerCase(hypothesisArgument.getPredicate().getPredicate()), 
				predicateLemmasLowerCase(textArgument.getPredicate().getPredicate()),
				new LinkedHashSet<String>()
				).size()>0);
	}
	
	private Set<String> predicateLemmasLowerCase(Predicate<I, S> predicate)
	{
		String mainLemma = InfoGetFields.getLemma(predicate.getHead().getInfo()).toLowerCase();
		if (null==predicate.getVerbsForNominal())
		{
			return Collections.singleton(mainLemma);
		}
		else
		{
			Set<String> ret = new LinkedHashSet<>();
			ret.add(mainLemma);
			for (String verbal : predicate.getVerbsForNominal())
			{
				ret.add(verbal.toLowerCase());
			}
			return ret;
		}
	}
	
	
	private Set<String> contentLemmasOfArgument_lowerCase(TypedArgument<I, S> argument)
	{
		return contentLemmasOfNodes_lowerCase(argument.getArgument().getNodes());
	}

	private Set<String> contentLemmasOfNodes_lowerCase(Iterable<S> nodes)
	{
		return contentLemmasOfNodes_lowerCase(nodes,null);
	}

	private Set<String> contentLemmasOfNodes_lowerCase(Iterable<S> nodes, Set<S> exclude)
	{
		Set<String> ret = new LinkedHashSet<>();
		for (S node : nodes)
		{
			if (!setContains(exclude,node))
			{
				if (InfoObservations.infoIsContentWord(node.getInfo()))
				{
					String lemma = InfoGetFields.getLemma(node.getInfo()).toLowerCase();
					if (!(stopWords.contains(lemma)))
					{
						ret.add(lemma);
					}
				}
			}
		}
		return ret;
	}
	
	private static final <T> boolean setContains(Set<T> set, T t)
	{
		if (null==set) return false;
		else return (set.contains(t));
	}
	
	
	private Set<S> getPredicateNodes(Set<PredicateArgumentStructure<I, S>> structures)
	{
		Set<S> ret = new LinkedHashSet<>();
		for (PredicateArgumentStructure<I, S> structure : structures)
		{
			ret.add(structure.getPredicate().getHead());
			//ret.addAll(structure.getPredicate().getNodes()); - wrong. "not sell" makes "not" as a predicate node.
		}
		return ret;
	}
	

	
	
//	private void fillContentWordSets(Iterable<S> nodes, Set<String> lemmas, Set<CanonicalLemmaAndPos> lemmaAndPoses) throws TeEngineMlException
//	{
//		if (lemmas!=null){lemmas.clear();}
//		if (lemmaAndPoses!=null){lemmaAndPoses.clear();}
//		for (S node : nodes)
//		{
//			if (InfoObservations.infoIsContentWord(node.getInfo()))
//			{
//				if (lemmas!=null){lemmas.add(InfoGetFields.getLemma(node.getInfo()).toLowerCase());}
//				if (lemmaAndPoses!=null){lemmaAndPoses.add(
//						new CanonicalLemmaAndPos( InfoGetFields.getLemma(node.getInfo()).toLowerCase(),InfoGetFields.getPartOfSpeechObject(node.getInfo()) )
//						);}
//			}
//		}
//
//	}

	
	private Set<String> allLemmasOfArgument_lowerCase(TypedArgument<I, S> argument)
	{
		Set<String> ret = new LinkedHashSet<>();
		for (S node : argument.getArgument().getNodes())
		{
			ret.add(InfoGetFields.getLemma(node.getInfo()).toLowerCase());
		}
		return ret;
	}
	
	
	
	
	// input
	private final TreeAndParentMap<I, S> hypothesisTree;
	private final Set<PredicateArgumentStructure<I, S>> hypothesisStructures;
	private final TreeAndParentMap<I, S> textTree;
	private final Set<PredicateArgumentStructure<I, S>> textStructures;
	private final ImmutableSet<String> stopWords;
	
	
	// internals
	private Set<String> contentLemmasOfHypothesisNonPredicates_lowerCase;
	private Set<String> contentLemmasOfHypothesisPredicates_lowerCase;
	private Set<String> lemmasOfText_lowerCase;
	private ValueSetMap<PredicateAndArgument<I, S>, PredicateAndArgument<I, S>> mapArgumentsHypothesisToText;
	private List<PredicateAndArgument<I, S>> hypothesisArguments;
	private List<PredicateAndArgument<I, S>> textArguments;
	
	// output
	private List<PredicateAndArgument<I, S>> calculatedNoMatchNamedEntities;
	private List<PredicateAndArgument<I, S>> calculatedNoMatch;
	private List<PredicateAndArgument<I, S>> calculatedMatchWrongPredicateMissingWords;
	private List<PredicateAndArgument<I, S>> calculatedMatchWrongPredicate;
	private List<PredicateAndArgument<I, S>> calculatedMatchMissingWords;
	private List<PredicateAndArgument<I, S>> calculatedMatch;
	private Set<String> calculatedTotallyOmittedHypothesisContentLemmasNonPredicates;
	private Set<String> calculatedTotallyOmittedHypothesisContentLemmasPredicates;
}
