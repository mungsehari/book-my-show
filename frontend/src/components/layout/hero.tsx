import { PopcornIcon, SparklesIcon, TicketIcon } from "lucide-react";

export function Hero() {
  return (
    <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-rose-600 via-red-500 to-orange-500 p-8 text-white shadow-xl sm:p-12">
      <PopcornIcon className="animate-float absolute -right-4 top-6 size-28 text-white/15 sm:size-44" />
      <TicketIcon
        className="animate-float absolute bottom-4 right-24 size-16 text-white/20"
        style={{ animationDelay: "1.5s" }}
      />
      <div className="relative max-w-xl space-y-4">
        <p
          className="animate-fade-up inline-flex items-center gap-1.5 rounded-full bg-white/20 px-3 py-1 text-xs font-medium backdrop-blur"
          style={{ ["--delay" as string]: "0ms" }}
        >
          <SparklesIcon className="size-3.5" /> Movies, made easy
        </p>
        <h1
          className="animate-fade-up text-4xl font-extrabold leading-tight sm:text-5xl"
          style={{ ["--delay" as string]: "100ms" }}
        >
          Your seat is waiting.
          <br />
          Book in seconds.
        </h1>
        <p
          className="animate-fade-up text-white/90"
          style={{ ["--delay" as string]: "200ms" }}
        >
          Pick your city, choose a movie, grab the best seats. No hidden fees at
          checkout.
        </p>
      </div>
    </section>
  );
}
