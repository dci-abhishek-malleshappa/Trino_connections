package com.example.trino.tally;

import io.trino.spi.connector.SchemaTableName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TallyTableRegistry
{
    public static final String SCHEMA_NAME = "default";

    private static final List<TallyTableDefinition> TABLES = List.of(
            table("ledgers", "Ledger", "LEDGER",
                    varchar("ledger_name", "Name", "NAME", "LEDGER_NAME"),
                    varchar("parent_group", "Parent", "PARENT", "PARENT_GROUP"),
                    doubleColumn("closing_balance", "ClosingBalance", "CLOSINGBALANCE", "CLOSING_BALANCE", "OPENINGBALANCE"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("groups", "Group", "GROUP",
                    varchar("group_name", "Name", "NAME", "GROUP_NAME"),
                    varchar("parent_group", "Parent", "PARENT"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("currencies", "Currency", "CURRENCY",
                    varchar("currency_name", "Name", "NAME"),
                    varchar("symbol", "Symbol", "SYMBOL"),
                    varchar("formal_name", "FormalName", "FORMALNAME", "FORMAL_NAME"),
                    varchar("decimal_symbol", "DecimalSymbol", "DECIMALSYMBOL", "DECIMAL_SYMBOL"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("voucher_types", "Voucher Type", "VOUCHERTYPE",
                    varchar("voucher_type_name", "Name", "NAME"),
                    varchar("parent", "Parent", "PARENT"),
                    varchar("reserved_name", "ReservedName", "RESERVEDNAME", "RESERVED_NAME"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("stock_groups", "Stock Group", "STOCKGROUP",
                    varchar("stock_group_name", "Name", "NAME"),
                    varchar("parent_group", "Parent", "PARENT"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("stock_items", "Stock Item", "STOCKITEM",
                    varchar("stock_item_name", "Name", "NAME"),
                    varchar("parent_group", "Parent", "PARENT"),
                    varchar("base_units", "BaseUnits", "BASEUNITS", "BASE_UNITS"),
                    doubleColumn("opening_balance", "OpeningBalance", "OPENINGBALANCE", "OPENING_BALANCE"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("units", "Unit", "UNIT",
                    varchar("unit_name", "Name", "NAME"),
                    varchar("original_name", "OriginalName", "ORIGINALNAME", "ORIGINAL_NAME"),
                    varchar("base_units", "BaseUnits", "BASEUNITS", "BASE_UNITS"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("godowns", "Godown", "GODOWN",
                    varchar("godown_name", "Name", "NAME"),
                    varchar("parent", "Parent", "PARENT"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("cost_categories", "Cost Category", "COSTCATEGORY",
                    varchar("cost_category_name", "Name", "NAME"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")),
            table("cost_centres", "Cost Centre", "COSTCENTRE",
                    varchar("cost_centre_name", "Name", "NAME"),
                    varchar("parent", "Parent", "PARENT"),
                    varchar("category", "Category", "CATEGORY"),
                    varchar("guid", "GUID", "GUID"),
                    varchar("master_id", "MasterID", "MASTERID")));

    private static final Map<SchemaTableName, TallyTableDefinition> TABLES_BY_NAME = TABLES.stream()
            .collect(LinkedHashMap::new, (map, table) -> map.put(table.getSchemaTableName(), table), Map::putAll);

    private TallyTableRegistry()
    {
    }

    public static List<TallyTableDefinition> listTables()
    {
        return TABLES;
    }

    public static Optional<TallyTableDefinition> getTable(SchemaTableName tableName)
    {
        return Optional.ofNullable(TABLES_BY_NAME.get(tableName));
    }

    public static Optional<TallyTableDefinition> getTable(String schemaName, String tableName)
    {
        return getTable(new SchemaTableName(schemaName, tableName));
    }

    private static TallyTableDefinition table(
            String tableName,
            String tallyType,
            String rowTagName,
            TallyColumnDefinition... columns)
    {
        return new TallyTableDefinition(SCHEMA_NAME, tableName, tallyType, rowTagName, List.of(columns));
    }

    private static TallyColumnDefinition varchar(String columnName, String nativeMethod, String... sourceNames)
    {
        return new TallyColumnDefinition(columnName, "varchar", nativeMethod, List.of(sourceNames));
    }

    private static TallyColumnDefinition doubleColumn(String columnName, String nativeMethod, String... sourceNames)
    {
        return new TallyColumnDefinition(columnName, "double", nativeMethod, List.of(sourceNames));
    }
}
