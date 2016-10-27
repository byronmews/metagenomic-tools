# metagenomic-tools

Pipeline and wrappers for metagenomic analysis

Usage examples
--------------

1. Kraken using bpipe workflow
--------------
Expects PE fastq files. All PE fastq within folder are analysed, using the string before illumina sample index number as sample ID.

  $ bpipe run -r kraken_pe.groovy *
  
Produces standard Kraken output and additional charts, with the suffixes:
 
- kraken.out.txt: labelled reads
- unclassified.fasta: unclassfied sequences
- kraken.report.txt, kraken.report_mpa.txt: kraken and mpa format reports
- kraken.out.krona.html: Krona Chart of taxonomic distributions

2. Metaphlan2 using bpipe
--------------
Expects PE fastq files. All PE fastq within folder are analysed, using the string before illumina sample index number as sample ID.

  $ bpipe run -r metaphlan2.groovy *

Produces standard Metaphlan profile, merges tables, and generates heatmaps at different taxonomic levels. Suffixes are:

- bowtie2.bz2: bowtie2 mapping for quicker rerun of later analysis stages
- profiled_metagenome.txt: each sample profile
- merged_abundance_table.txt: all sample profiles merged into one table
- heatmaps/merged_abundance_table.[phylum|family|genus|species].png: heatmap, with columns clustered based on braycurtis distances, rows by correlation.
  
  
  
  
