// QIIME PE run for qc passed, screened reads and joined
//
// Usage: bpipe run qiime.py *
// Author: Graham Rose
//

MAPPING_FILE="faecal_dataset_N73_meta.tsv"


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
	
	// First block. Write two files, striping first header line in $MAPPING_FILE
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

// Remove chimeras, using the unclustered demultiplexed sequences
chimera_removal={

}





Bpipe.run {
	validate_mapping + qiime_split
}
