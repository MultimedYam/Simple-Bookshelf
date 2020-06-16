package io.yamil.bookshelf;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.material.MaterialData;

import java.sql.*;
import java.util.Base64;

import static io.yamil.bookshelf.Bookshelf.instance;

public class PlayerListener implements Listener {

    @EventHandler
    public void onBookshelfBreak(BlockBreakEvent e) throws SQLException
    {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        int posX = (int) block.getLocation().getX();
        int posY = (int) block.getLocation().getY();
        int posZ = (int) block.getLocation().getZ();

        if (e.getBlock().getType().equals(Material.BOOKSHELF))
        {
            ItemStack itemToSpawn = RemoveBookFromShelf(player.getWorld().getName(), posX, posY, posZ);
            if (itemToSpawn != null)
            {
                player.getWorld().dropItemNaturally(new Location(player.getWorld(), posX, posY, posZ), itemToSpawn);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) throws SQLException
    {
        Player player = e.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Action action = e.getAction();
        Block block = e.getClickedBlock();
        if (block == null) return;
        int posX = (int) block.getLocation().getX();
        int posY = (int) block.getLocation().getY();
        int posZ = (int) block.getLocation().getZ();

        if (block.getType().equals(Material.BOOKSHELF))
        {
            //If player is author of book in bookshelf, remove book from bookshelf
            boolean crouchLeftClick = player.isSneaking() && action.equals(Action.LEFT_CLICK_BLOCK);

            //Make copy of book in hand
            boolean leftClickWithBook = itemInHand.getType().equals(Material.WRITABLE_BOOK) && action.equals(Action.LEFT_CLICK_BLOCK);

            //Retrieve info book in shelf
            boolean leftClickOpenArms =
                    (itemInHand == null || itemInHand.getType().equals(Material.AIR)) && action.equals(Action.LEFT_CLICK_BLOCK);

            //No book in shelf, written book in hand, put book in bookshelf
            boolean leftClickWithWrittenBook =
                    itemInHand.getType().equals(Material.WRITTEN_BOOK) && action.equals(Action.LEFT_CLICK_BLOCK);

            if (crouchLeftClick)
            {
                ItemStack itemToSpawn = RemoveBookFromShelf(player.getWorld().getName(), posX, posY, posZ);
                if (itemToSpawn != null)
                {
                    player.getWorld().dropItemNaturally(new Location(player.getWorld(), posX, posY, posZ), itemToSpawn);
                    player.sendMessage(ChatColor.AQUA + "You feel as though this book does not belong here anymore.");
                }
            }
            else if (leftClickWithBook)
            {
                ItemStack copiedBook = CopyBook(player.getWorld().getName(), posX, posY, posZ);
                if (copiedBook != null)
                {
                    player.sendMessage(ChatColor.AQUA + "You feel as though you copy the book in the shelf.");
                    player.getInventory().setItemInMainHand(copiedBook);
                }
                else
                {
                    return;
                }
            }
            else if (leftClickOpenArms)
            {
                //player.sendMessage("Getting book info");
                WrittenBook book = GetBookInfoFromShelf(player.getWorld().getName(), posX, posY, posZ);
                if (book != null)
                {
                    player.sendMessage(ChatColor.AQUA + "You feel as though there is a book in this bookshelf.");
                    player.sendMessage(ChatColor.AQUA + "Title: " + ChatColor.GOLD + book.Title);
                    player.sendMessage(ChatColor.AQUA + "By: " + ChatColor.GRAY + book.Author);
                }
                else
                {
                    player.sendMessage(ChatColor.AQUA + "You feel as though there is no book in this bookshelf.");
                }
            }
            else if (leftClickWithWrittenBook)
            {
                BookMeta bookMeta = (BookMeta) itemInHand.getItemMeta();
                WrittenBook book = new WrittenBook(bookMeta.getTitle(), bookMeta.getAuthor(), bookMeta.getPages(),
                        bookMeta.getPageCount());
                if (AddBookToShelf(player.getWorld().getName(), posX, posY, posZ, book))
                {
                    player.sendMessage(ChatColor.AQUA + "You feel as though this books belongs on this bookshelf.");
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                }
                else
                {
                    player.sendMessage(ChatColor.AQUA + "You feel as though this bookshelf already contains a book.");
                }
            }
            else return;

        }
        else return;
    }

    private ItemStack RemoveBookFromShelf(String world, int x, int y, int z) throws SQLException
    {
        //Encode XYZ to get private key
        //Find key in DB correct table
        //If key found, create item entity with book data, remove book from db
        //If key not found, no book in shelf
        ItemStack itemToSpawn = CopyBook(world, x, y, z);

        Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/_bookshelves.db");
        String bookId = "x:" + x + "y:" + y + "z:" + z;
        String bookEncode = Base64.getEncoder().encodeToString(bookId.getBytes());
        int bookPages = 0;
        try
        {
            if (true)
            {
                Statement stmt = conn.createStatement();
                ResultSet result =
                        stmt.executeQuery("SELECT * FROM " + world + "_bookshelves WHERE id = '" + bookEncode + "';");
                if (result.next())
                {
                    bookPages = result.getInt("pages");
                }
                else
                {
                    conn.close();
                    return null;
                }

                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM " + world + "_bookshelves WHERE id = '" + bookEncode + "';");
                stmt.close();

                int currentPage = 1;
                while (currentPage <= bookPages)
                {
                    stmt = conn.createStatement();
                    stmt.executeUpdate("DELETE FROM " + world + "_books WHERE id = '" + bookEncode + "." + currentPage + "';");
                    currentPage++;
                    stmt.close();
                }
            }
        } catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        conn.close();
        return itemToSpawn;
    }

    private WrittenBook GetBookInfoFromShelf(String world, int x, int y, int z) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/_bookshelves.db");
        String bookId = "x:" + x + "y:" + y + "z:" + z;
        String bookEncode = Base64.getEncoder().encodeToString(bookId.getBytes());
        try
        {
            if (!conn.isClosed())
            {
                Statement stmt = conn.createStatement();
                ResultSet result =
                        stmt.executeQuery("SELECT * FROM " + world + "_bookshelves WHERE id = '" + bookEncode + "';");

                if (result.next())
                {
                    //Book found in shelf
                    String bookID = result.getString("id");
                    String bookTitle = result.getString("title");
                    String bookAuthor = result.getString("author");
                    int bookPages = result.getInt("pages");

                    conn.close();
                    return new WrittenBook(bookTitle, bookAuthor, null, bookPages);
                }
            }
        } catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }

        conn.close();
        return null;
    }

    private boolean AddBookToShelf(String world, int x, int y, int z, WrittenBook book) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/_bookshelves.db");
        String bookId = "x:" + x + "y:" + y + "z:" + z;
        String bookEncode = Base64.getEncoder().encodeToString(bookId.getBytes());

        try
        {
            if (!conn.isClosed())
            {
                Statement stmt = conn.createStatement();
                ResultSet result =
                        stmt.executeQuery("SELECT * FROM " + world + "_bookshelves WHERE id = '" + bookEncode + "';");

                if (result.next())
                {
                    String bookID = result.getString("id");
                }
                else
                {
                    stmt.executeUpdate(
                            "INSERT INTO " + world +
                                    "_bookshelves " +
                                    "VALUES('" +
                                    bookEncode + "', " +
                                    x + ", " +
                                    y + ", " +
                                    z + ", '" +
                                    book.Author + "', '" +
                                    book.Title + "', " +
                                    book.totalPages + ");"

                    );

                    int currentPage = 1;
                    while (currentPage <= book.totalPages)
                    {
                        String content = book.Pages.get(currentPage-1);
                        stmt.executeUpdate(
                                "INSERT INTO " + world +
                                        "_books " +
                                        "VALUES('" +
                                        bookEncode + "." + currentPage + "', '" +
                                        bookEncode + "', " +
                                        currentPage + ", '" +
                                        content + "');"
                        );
                        currentPage++;
                    }
                    conn.close();
                    return true;
                }
            }
        } catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        conn.close();
        return false;
    }

    private ItemStack CopyBook(String world, int x, int y, int z) throws SQLException
    {
        WrittenBook newBook = null;
        ItemStack bookItem = null;
        BookMeta bookData = null;//(BookMeta) bookItem.getItemMeta();

        Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/_bookshelves.db");
        String bookId = "x:" + x + "y:" + y + "z:" + z;
        String bookEncode = Base64.getEncoder().encodeToString(bookId.getBytes());
        try
        {
            if (!conn.isClosed())
            {
                Statement stmt = conn.createStatement();
                ResultSet result =
                        stmt.executeQuery("SELECT * FROM " + world + "_bookshelves WHERE id = '" + bookEncode + "';");

                if (result.next())
                {
                    //Book found in shelf
                    String bookTitle = result.getString("title");
                    String bookAuthor = result.getString("author");
                    int bookPages = result.getInt("pages");
                    newBook = new WrittenBook(bookTitle, bookAuthor, null, bookPages);
                }
                else
                {
                    conn.close();
                    return null;
                }

                int currentPage = 1;
                while (currentPage <= newBook.totalPages)
                {
                    stmt = conn.createStatement();
                    ResultSet pageResult =
                            stmt.executeQuery("SELECT * FROM " + world + "_books WHERE id = '" + bookEncode + "." + currentPage + "';");

                    if (pageResult.next())
                    {
                        String content = pageResult.getString("content");
                        newBook.Pages.add(content);
                    }
                    currentPage++;
                }

                if (newBook != null)
                {
                    bookItem = new ItemStack(Material.WRITTEN_BOOK);
                    bookData = (BookMeta)bookItem.getItemMeta();
                    bookData.setTitle(newBook.Title);
                    bookData.setAuthor(newBook.Author);
                    bookData.setPages(newBook.Pages);
                    bookItem.setItemMeta(bookData);

                    conn.close();
                    return bookItem;
                }
            }
        } catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }

        conn.close();
        return bookItem;
    }
}
