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

	doc ""

	input_extension = ".txt"

	products = [
	("qiime_split_lib_sample_ids.txt"),
	("qiime_split_lib_sample_fastqs.txt")
	]
	
	produce(products) {
		exec "awk '{print \$1}' $MAPPING_FILE | tr '\\n' ',' > $output1"
		exec "awk '{print \$10}' $MAPPING_FILE | tr '\\n' ',' > $output2"
	}
}

Bpipe.run {
	validate_mapping + qiime_split
}
