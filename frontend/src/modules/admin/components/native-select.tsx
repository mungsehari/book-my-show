import type { ComponentProps } from "react";

type Option = { value: string | number; label: string };

type Props = Omit<ComponentProps<"select">, "children"> & {
  label: string;
  options: Option[];
  placeholder?: string;
};

/** A plain select with a label. Works well on phones. */
export function NativeSelect({ label, options, placeholder, id, ...rest }: Props) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <select
        id={id}
        className="h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
        {...rest}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}
