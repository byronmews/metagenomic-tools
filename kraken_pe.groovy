// Classification of unassembled reads using Kraken and post data processing
//
// Workflow: Kraken > Reporting > Krona Chart generation
// Usage: bpipe run -r kraken_pe.groovy *
// Author: Graham Rose
//

KRAKEN_CUSTOM_DB="/srv/data0/dbs/kraken/standard_220317"

// List all PE fastq input 
samples_in={

	doc "List all input PE fastq files"
		
	exec "echo R1: $input1 R2: $input2"

	forward input1, input2
}

// Kraken PE mode
kraken={
	
	doc "Kraken taxonomic classifier using exact kmers"

	input_extension = ".fastq"

	products = [
	("$input1".replaceAll(/_L001_.*/,"") - input_extension + '.unclassified.fasta'),
	("$input1".replaceAll(/_L001_.*/,"") - input_extension + '.kraken.out.txt')	
	]

	produce(products) {
		exec """
		kraken
		--db $KRAKEN_CUSTOM_DB
		--threads 10
		--quick
		--min-hits 4
		--check-names
		--fastq-input
		--paired
		--unclassified-out $output1.unclassified.fasta
		$input1.fastq $input2.fastq
		--output $output2.kraken.out.txt
		"""
	}
}

// Report standard format. Using --show-zerosopt for cross sample comparison
kraken_report={
	
	transform(".kraken.out.txt") to (".kraken.report.txt") {
		
		exec "kraken-report --show-zeros --db $KRAKEN_CUSTOM_DB $input.kraken.out.txt > $output.kraken.report.txt"
	}
	forward input.kraken.out.txt
}

// Report also using metaphlan format
kraken_mpa_report={
	
	transform(".out.txt") to (".report_mpa.txt") {
		
		exec "kraken-mpa-report --db $KRAKEN_CUSTOM_DB $input.kraken.out.txt > $output.kraken.report_mpa.txt"
	}
	forward input.kraken.out.txt
}

// Convert to ktTools ready format and import taxonomy to build krona chart
krona={
	exec "cut -f2,3 $input.kraken.out.txt | ktImportTaxonomy - -o $output.html "
}




// Run as single sample
//Bpipe.run {
//	samples_in + kraken + kraken_report + kraken_mpa_report + krona
//}

// Multiple samples where file names begin with sample name separated by regex
// such as: 1-2_S2_L001_R1_001.fastq)
Bpipe.run {
	"%_S*.fastq" * [ samples_in + kraken + kraken_report + kraken_mpa_report + krona ]
}



