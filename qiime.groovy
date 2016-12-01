// QIIME run for qc passed, screened reads and joined reads.
// 
// Everything runs from sample sheet, built from input file
//
// Workflow: validate_mapping_file.py > split_libraries_fastq.py > identify_chimeric_seqs.py 
// 			> filter_fasta.py > pick_open_reference_otus.py > core_diversity_analyses.py
//
// Usage: bpipe run qiime.py my_qiime_mapping_file.tsv
//
// Author: Graham Rose
//

import groovy.io.FileType

CHIMERA_DB="/usr/lib/python2.7/site-packages/qiime_default_reference/gg_13_8_otus/rep_set/97_otus.fasta"
SUBSAMPLE_DEPTH="5000"


// Validate mapping file, ignore barcode errors
validate_mapping={
	
	doc "Run validation of mapping file"

	exec "validate_mapping_file.py -o mapping_file_check -m $input -b -j Description"

	forward input
}

// Split fastqs ignoring demultiplexing option. Names extracted from input mapping file.
qiime_split={

	doc "Run split_libraries_fastq.py using sample IDs and fastqs filenames extracted from mapping file"

	// Generate 3 outputs. First block extracts sample IDs and names. Second block runs fastq splitter
	products = [
	("qiime_split_lib_sample_ids.txt"),
	("qiime_split_lib_sample_fastqs.txt"),
	("slout")
	]
	
	// First block. Format file inputs. Write two files, stripping first header line in input mapping file
	produce(products) {
		exec "awk '{if (NR!=1) {print \$1}}' $input | tr '\\n' ',' > $output1"
		exec "awk '{if (NR!=1) {print \$10}}' $input | tr '\\n' ',' > $output2"
	}

	// Create variables from above two filenames, removing orphan comma character at end of string
	String sampleIDs = new File('qiime_split_lib_sample_ids.txt').text
	sampleIDs = sampleIDs.substring(0, sampleIDs.length() - 1)

	String sampleFastqs = new File('qiime_split_lib_sample_fastqs.txt').text
	sampleFastqs = sampleFastqs.substring(0,sampleFastqs.length() - 1)	

	// Write files to bpipe stderr for debugging. Write as samples and fastq filenames as single column
	def sampleIDCol1 = sampleIDs.replaceAll(',','\n')
	def sampleFastqsCol2 = sampleFastqs.replaceAll(',','\n')
	printf("Sample IDs:\n" + sampleIDCol1 + "\n\n" + "Sample Fastqs:\n" + sampleFastqsCol2)

	// Second block. Run split_libraries_fastq.py using above variables. Adjust phred_offset value for project
	produce(products) {
		exec """
		split_libraries_fastq.py 
		-i $sampleFastqs
		--sample_id $sampleIDs 
		-o $output3 
		-m $input
		-q 19 
		--barcode_type 'not-barcoded'
		--phred_offset 33
		"""
	}
	forward input
}

// Remove chimeras using the unclustered demultiplexed sequences
// Splits sequence set to avoid out of memory errors as only 32bit usearch in use
chimera_removal={

	doc "Remove chimeric sequences using usearch61 and greengenes db"

	// Chimera output dir
	output.dir = "chimera_checked"	
	exec "mkdir -p $output.dir"	

	// Split combined fasta into files with 1M sequences, move to new directory
	exec "split -l 2000000 --additional-suffix .fna slout/seqs.fna chunk_ ; mv chunk* chimera_checked"

	// Cycle split fastas in directory, using suffix to find file
	produce("combined_chimeras.txt") {

		newDir = new File("chimera_checked")
		def seqs = ~/.*.fna/
		newDir.eachFileMatch(seqs) { file ->
		
			// Remove .fna suffix and create new directory for each split
			chimeraSplitDirName = file.getName()
			chimeraSplitDirName = chimeraSplitDirName.substring(0, chimeraSplitDirName.length() - 4)
			chimeraSplitDir = new File("chimera_checked/$chimeraSplitDirName")

			exec "echo Checking split: $chimeraSplitDir"	
	
			// Run usearch61 sequentially on splits, use multithreading
			exec """
				identify_chimeric_seqs.py -i $file
				-m usearch61
				--threads 12
				-o $chimeraSplitDir
				-r $CHIMERA_DB
			"""
		}
		// Merge indentified chimeras and write to file
		exec "cat chimera_checked/*/chimeras.txt >> $newDir/combined_chimeras.txt"
	}
	forward input
}

// Remove chimeric reads from sequence set
filter_fasta={
	
	doc "Filter fasta of chimeric sequences"

	output.dir = "slout"	

	produce("seqs_chimeras_filtered.fna") {
	
		exec """
			filter_fasta.py -f slout/seqs.fna
			-o $output.dir/seqs_chimeras_filtered.fna
			-s chimera_checked/combined_chimeras.txt -n
		"""
	}
	forward input
}

// Pick OTUs using open reference method and Greengenes db
pick_otus={

	doc "Pick OTUs using open reference method and GreenGenes database clusted at 97% sequence identity"


	produce("otus") {
	
		exec """
			pick_open_reference_otus.py
			--parallel
			--jobs_to_start 12
			-o otus
			-i slout/seqs_chimeras_filtered.fna
			-p qiime_parameter_file.txt
		"""
	}
	forward input
}

// Run a basic core diveristy analysis, using hardset subsampled read depth for first run
core_diversity={

	doc "Summarising biom and analyse core diverity using default depth of $SUBSAMPLE_DEPTH reads"

	// Summarise biom file listing read counts per sample	
	exec "biom summarize-table -i otus/otu_table_mc2_w_tax_no_pynast_failures.biom"

	// Run core diversity analysis, default to set mapping depth
	exec """
		core_diversity_analyses.py -o cdout/
		--parallel -O 12
		-p qiime_parameter_file.txt
		--recover_from_failure
		-i otus/otu_table_mc2_w_tax_no_pynast_failures.biom
		-m $input
		-t otus/rep_set.tre
		-e $SUBSAMPLE_DEPTH
	"""
}


// Input qiime formatted sample sheet file. Everything runs from sample sheet, built from input file, parallelism where possible
Bpipe.run {
	validate_mapping + qiime_split + chimera_removal + filter_fasta + pick_otus + core_diversity
}



