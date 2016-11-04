// QIIME PE run for qc passed, screened reads and joined
//
// Usage: bpipe run qiime.py *
// Author: Graham Rose
//

import groovy.io.FileType

MAPPING_FILE="faecal_dataset_N73_meta.tsv"
CHIMERA_DB="/usr/lib/python2.7/site-packages/qiime_default_reference/gg_13_8_otus/rep_set/97_otus.fasta"


// Validate mapping file, ignore barcode errors
validate_mapping={
	
	doc "Run validation of mapping file"

	exec "validate_mapping_file.py -o mapping_file_check -m $MAPPING_FILE -b -j Description"
}

// Split fastqs ignoring demultiplexing option. Names extracted from mapping file.
qiime_split={

	doc "Run split_libraries_fastq.py using sample IDs and fastqs filenames extracted from mapping file"

	// Generate 3 outputs. First block extracts sample IDs and names. Second block runs fastq splitter.
	products = [
	("qiime_split_lib_sample_ids.txt"),
	("qiime_split_lib_sample_fastqs.txt"),
	("slout")
	]
	
	// First block. Write two files, stripping first header line in $MAPPING_FILE
	produce(products) {
		exec "awk '{if (NR!=1) {print \$1}}' $MAPPING_FILE | tr '\\n' ',' > $output1"
		exec "awk '{if (NR!=1) {print \$10}}' $MAPPING_FILE | tr '\\n' ',' > $output2"
	}

	// Read files to vars, removing orphan comman char in string
	String sampleIDs = new File('qiime_split_lib_sample_ids.txt').text
	sampleIDs = sampleIDs.substring(0, sampleIDs.length() - 1)

	String sampleFastqs = new File('qiime_split_lib_sample_fastqs.txt').text
	sampleFastqs = sampleFastqs.substring(0,sampleFastqs.length() - 1)	

	// Write files to bpipe stderr as debug
	def sampleIDCol1 = sampleIDs.replaceAll(',','\n')
	def sampleFastqsCol2 = sampleFastqs.replaceAll(',','\n')
	printf("Sample IDs:\n" + sampleIDCol1 + "\n\n" + "Sample Fastqs:\n" + sampleFastqsCol2) 

	// Second block. Run split_libraries_fastq.py using above variables.
	produce(products) {
		exec """
		split_libraries_fastq.py 
		-i $sampleFastqs
		--sample_id $sampleIDs 
		-o $output3 
		-m $MAPPING_FILE
		-q 19 
		--barcode_type 'not-barcoded'
		"""
	}	
}

// Remove chimeras, using the unclustered demultiplexed sequences. Splits input to avoid  out of memory error
chimera_removal={

	doc "Remove chimeric sequences using usearch61 and greengenes db"

	// Chimera output dir
	output.dir = "chimera_checked"	
	exec "mkdir -p $output.dir"	

	// Split combined fasta into files with 1M seqs, mv to new dir
	exec "split -l 2000000 --additional-suffix .fna slout/seqs.fna chunk_ ; mv chunk* chimera_checked"

	// Cycle split fastas in dir, using suffix to find file
	newDir = new File("chimera_checked")
	def seqs = ~/.*.fna/
	newDir.eachFileMatch(seqs) { file ->
		
		println file.getName()
		
		// Remove .fna suffix and create new dir
		chimeraSplitDirName = file.getName()
		chimeraSplitDirName = chimeraSplitDirName.substring(0, chimeraSplitDirName.length() - 4)
		chimeraSplitDir = new File("chimera_checked/$chimeraSplitDirName")
		
		// Run usearch61 sequentially on splits, use multithreading
		produce("$newDir/combined_chimeras.txt") {
			exec """
				identify_chimeric_seqs.py -i $file
				-m usearch61
				--threads 12
				-o $chimeraSplitDir
				-r $CHIMERA_DB
			"""
	
			// Merge indentified chimeras
			exec "cat $chimeraSplitDir/chimeras.txt >> $newDir/combined_chimeras.txt"
		}
	}
}

filter_fasta={
	
	doc "Filter fasta of chimeric sequences"

	output.dir = "slout"	

	exec """
		filter_fasta.py -f slout/seqs.fna
		-o $output.dir/seqs_chimeras_filtered.fna
		-s chimera_checked/combined_chimeras.txt -n
	"""
}


// No input, runs off qiime sample sheet, parallelism where possible
Bpipe.run {
	validate_mapping + qiime_split + chimera_removal + filter_fasta
}
