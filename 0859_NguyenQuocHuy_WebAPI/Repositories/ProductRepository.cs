using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Repositories;

/// <summary>
/// Triển khai các phương thức truy vấn và cập nhật dữ liệu Sản phẩm
/// </summary>
public class ProductRepository : IproductRepository
{
    private readonly ApplicationDbContext _dbContext;

    public ProductRepository(ApplicationDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<IEnumerable<Product>> GetProductsAsync()
    {
        return await GetProductsAsync(null, null, null, null, null);
    }

    public async Task<IEnumerable<Product>> GetProductsAsync(
        string? search, 
        int? categoryId, 
        decimal? minPrice, 
        decimal? maxPrice, 
        string? sortBy)
    {
        var query = _dbContext.Products
            .AsNoTracking()
            .Include(item => item.Category)
            .AsQueryable();

        // 1. Tìm kiếm theo tên hoặc mô tả
        if (!string.IsNullOrWhiteSpace(search))
        {
            var normalizedSearch = search.Trim().ToLower();
            query = query.Where(p => 
                (p.Name != null && p.Name.ToLower().Contains(normalizedSearch)) ||
                (p.Description != null && p.Description.ToLower().Contains(normalizedSearch)));
        }

        // 2. Lọc theo danh mục
        if (categoryId.HasValue && categoryId.Value > 0)
        {
            query = query.Where(p => p.CategoryId == categoryId.Value);
        }

        // 3. Lọc theo khoảng giá
        if (minPrice.HasValue && minPrice.Value >= 0)
        {
            query = query.Where(p => p.Price >= minPrice.Value);
        }
        if (maxPrice.HasValue && maxPrice.Value > 0)
        {
            query = query.Where(p => p.Price <= maxPrice.Value);
        }

        // 4. Sắp xếp
        query = (sortBy?.ToLower()) switch
        {
            "price_asc" => query.OrderBy(p => p.Price),
            "price_desc" => query.OrderByDescending(p => p.Price),
            "name_desc" => query.OrderByDescending(p => p.Name),
            "name_asc" => query.OrderBy(p => p.Name),
            _ => query.OrderByDescending(p => p.Id)
        };

        return await query.ToListAsync();
    }

    public async Task<IEnumerable<Product>> GetProductsByCategoryIdAsync(int categoryId)
    {
        return await _dbContext.Products
            .AsNoTracking()
            .Include(item => item.Category)
            .Where(p => p.CategoryId == categoryId)
            .OrderBy(p => p.Name)
            .ToListAsync();
    }

    public async Task<Product?> GetProductByIdAsync(int id)
    {
        return await _dbContext.Products
            .AsNoTracking()
            .Include(item => item.Category)
            .SingleOrDefaultAsync(item => item.Id == id);
    }

    public async Task AddProductAsync(Product product)
    {
        ArgumentNullException.ThrowIfNull(product);
        await _dbContext.Products.AddAsync(product);
        await _dbContext.SaveChangesAsync();
    }

    public async Task UpdateProductAsync(Product product)
    {
        ArgumentNullException.ThrowIfNull(product);
        _dbContext.Products.Update(product);
        await _dbContext.SaveChangesAsync();
    }

    public async Task DeleteProductAsync(int id)
    {
        var targetProduct = await _dbContext.Products.FindAsync(id);
        if (targetProduct is not null)
        {
            _dbContext.Products.Remove(targetProduct);
            await _dbContext.SaveChangesAsync();
        }
    }
}
