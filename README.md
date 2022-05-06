# gsheet-as-db

##Write formula
Mark the field as formula=true.
Ex: 
@Column(name = "Name", order = 2, formula=true)
public String name;

To refer the row number use =indirect() and =row() formulas.
Ex: =INDIRECT("A" & ROW())
& is used join column and row number to refer a cell.
