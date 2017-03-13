Pipeline and wrappers for metagenomic analysis
--------------

Usage examples:
--------------

1. Kraken using bpipe workflow
--------------
Expects PE fastq files. All PE fastq within folder are analysed, using the string before the illumina sample index number as the sample ID.

  $ bpipe run -r kraken_pe.groovy *
  
Produces standard Kraken output and additional charts, with the suffixes:
 
- kraken.out.txt: labelled reads
- unclassified.fasta: unclassfied sequences
- kraken.report.txt, kraken.report_mpa.txt: kraken and mpa format reports
- kraken.out.krona.html: Krona Chart of taxonomic distributions

2. Metaphlan2 using bpipe
--------------

Requires: metaphlan_to_stamp.pl

Expects PE fastq files. All PE fastq within folder are analysed, using the string before the illumina sample index number as sample ID.

  $ bpipe run -r metaphlan2.groovy *

Produces standard Metaphlan profile, merges tables, and generates heatmaps at different taxonomic levels. Filename suffixes are:

- bowtie2.bz2: bowtie2 mapping for quicker rerun of later analysis stages
- profiled_metagenome.txt: each sample profile
- merged_abundance_table.txt: all sample profiles merged into one table
- heatmaps/merged_abundance_table.[phylum|family|genus|species].png: heatmap, with columns clustered based on braycurtis distances, rows by correlation.

3. QIIME core diversity analysis using bpipe
--------------
  
Expects joined fastq files within the directory, a qiime parameter file, and qiime sample mapping file (mapping file is given at commandline). All fastq within the sample mappng file are used in the analysis. 

Sample ID is taken from column 1 of the mapping file, and corresponding fastq files within column 10. The remaining columns follow the standard format (see below).

  $ bpipe run -r qiime.py my_qiime_mapping_file.tsv
  
Produces the following files with suffixes:

 - mapping_file_check: check mapping file
 - slout: combined and labelled demultiplexed sequences
 - chimeria_checked: chimeric sequences identified within chunks of 1M sequences, and filtered from core fasta file
 - otus: OTU picking using open reference method and green genes (clutsered at default at 97% sequence identity)
 - cdout: core diversity analysis, using a hard set 5000 read depth
 
 
Example QIIME mapping file format:

SampleID |	BarcodeSequence |	LinkerPrimerSequence  |	SampleIndex	|	SampleType	|	AmpliconType	|	SampleRun	|	GRUID	|	DRWFNumber	|	FileName	|	Description
------------ | ------------- | ------------ | ------------- | ------------ | ------------- | ------------ | ------------- | ------------ | ------------- |
1F.2	TCCTGAGC	GTAAGGAG	1	F	V3-V4	nafld_040716	1F	001	1F-3-4.exFrags.trimmed.fastq	1F-3-4_S1


